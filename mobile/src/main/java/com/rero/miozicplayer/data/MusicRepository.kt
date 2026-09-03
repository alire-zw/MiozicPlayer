package com.rero.miozicplayer.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    suspend fun scanAllMusic(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        songs.addAll(scanMediaStore())
        songs.addAll(scanUsbVolumes())
        songs.distinctBy { it.uri.toString() }.sortedBy { it.title.lowercase() }
    }

    suspend fun scanUsbMusic(): List<Song> = withContext(Dispatchers.IO) {
        scanUsbVolumes().distinctBy { it.uri.toString() }.sortedBy { it.title.lowercase() }
    }

    private fun scanMediaStore(): List<Song> {
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown"
                val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                val album = cursor.getString(albumCol) ?: "Unknown Album"
                val duration = cursor.getLong(durationCol)
                val albumId = cursor.getLong(albumIdCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId,
                )

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uri = uri,
                        albumArtUri = albumArtUri,
                        source = MusicSource.LOCAL,
                    ),
                )
            }
        }
        return songs
    }

    private fun scanUsbVolumes(): List<Song> {
        val songs = mutableListOf<Song>()
        val audioExtensions = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac", "wma")

        val roots = mutableListOf<File>()
        val storageDir = File("/storage")
        if (storageDir.exists()) {
            storageDir.listFiles()?.forEach { entry ->
                if (entry.isDirectory && entry.canRead() && entry.name !in EXCLUDED_STORAGE_NAMES) {
                    roots.add(entry)
                }
            }
        }

        val usbDir = File("/mnt/media_rw")
        if (usbDir.exists()) {
            usbDir.listFiles()?.forEach { entry ->
                if (entry.isDirectory && entry.canRead()) {
                    roots.add(entry)
                }
            }
        }

        Environment.getExternalStorageDirectory()?.let { roots.add(it) }

        roots.distinctBy { it.absolutePath }.forEach { root ->
            scanDirectory(root, audioExtensions, songs)
        }
        return songs
    }

    private fun scanDirectory(dir: File, extensions: Set<String>, songs: MutableList<Song>) {
        if (!dir.canRead()) return
        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory && !file.name.startsWith(".") -> scanDirectory(file, extensions, songs)
                file.isFile && file.extension.lowercase() in extensions -> {
                    songs.add(fileToSong(file))
                }
            }
        }
    }

    private fun fileToSong(file: File): Song {
        val id3Tags = file.inputStream().use { input ->
            AudioMetadataExtractor.readId3Tags(input)
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val metadata = AudioMetadataExtractor.fromRetriever(retriever, file.name, id3Tags)
            val albumArtUri = AlbumArtCache.extractFromRetriever(
                context,
                retriever,
                "file_${file.absolutePath.hashCode()}",
            )

            Song(
                id = file.absolutePath.hashCode().toLong(),
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                durationMs = metadata.durationMs,
                uri = Uri.fromFile(file),
                albumArtUri = albumArtUri,
                source = MusicSource.USB,
            )
        } catch (_: Exception) {
            Song(
                id = file.absolutePath.hashCode().toLong(),
                title = id3Tags?.title ?: file.nameWithoutExtension,
                artist = id3Tags?.artist ?: "Unknown Artist",
                album = id3Tags?.album ?: "Unknown Album",
                durationMs = 0L,
                uri = Uri.fromFile(file),
                source = MusicSource.USB,
            )
        } finally {
            retriever.release()
        }
    }

    suspend fun loadBrowseFolders(): List<BrowseFolder> = withContext(Dispatchers.IO) {
        val folders = mutableListOf<BrowseFolder>()
        val bundled = loadBundledPlaylist()
        folders.add(
            BrowseFolder(
                id = bundled.id,
                title = bundled.title,
                description = bundled.description,
                songs = bundled.songs,
            ),
        )
        val usbSongs = scanUsbMusic()
        if (usbSongs.isNotEmpty()) {
            folders.add(
                BrowseFolder(
                    id = "usb",
                    title = "USB Music",
                    description = "Songs from connected USB drive.",
                    songs = usbSongs,
                ),
            )
        }
        folders
    }

    suspend fun loadBundledPlaylist(): Playlist = withContext(Dispatchers.IO) {
        val songs = bundledTrackFiles.mapIndexed { index, fileName ->
            loadBundledSong(index.toLong() + 1, fileName)
        }
        Playlist(
            id = "bundled",
            title = "Road Trip Vibes",
            description = "A mix of upbeat and relaxing tunes for your next journey.",
            songs = songs,
        )
    }

    suspend fun loadBundledSongs(): List<Song> = withContext(Dispatchers.IO) {
        bundledTrackFiles.mapIndexed { index, fileName ->
            loadBundledSong(index.toLong() + 1, fileName)
        }
    }

    private fun loadBundledSong(id: Long, fileName: String): Song {
        val assetPath = "$ASSETS_MUSIC/$fileName"
        val uri = resolveBundledPlaybackUri(fileName)
        val id3Tags = context.assets.open(assetPath).use { input ->
            AudioMetadataExtractor.readId3Tags(input)
        }
        val retriever = MediaMetadataRetriever()

        return try {
            context.assets.openFd(assetPath).use { afd ->
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                val metadata = AudioMetadataExtractor.fromRetriever(retriever, fileName, id3Tags)
                val albumArtUri = AlbumArtCache.extractFromRetriever(
                    context,
                    retriever,
                    "bundled_$fileName",
                )

                Song(
                    id = id,
                    title = metadata.title,
                    artist = metadata.artist,
                    album = metadata.album,
                    durationMs = metadata.durationMs,
                    uri = uri,
                    albumArtUri = albumArtUri,
                    source = MusicSource.BUNDLED,
                )
            }
        } catch (_: Exception) {
            Song(
                id = id,
                title = id3Tags?.title ?: fileName.substringBeforeLast('.'),
                artist = id3Tags?.artist ?: "Unknown Artist",
                album = id3Tags?.album ?: "Road Trip Vibes",
                durationMs = 0L,
                uri = uri,
                source = MusicSource.BUNDLED,
            )
        } finally {
            retriever.release()
        }
    }

    private fun resolveBundledPlaybackUri(fileName: String): Uri {
        val assetPath = "$ASSETS_MUSIC/$fileName"
        val cacheDir = File(context.cacheDir, "bundled_music")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val cacheFile = File(cacheDir, fileName)
        if (!cacheFile.exists()) {
            context.assets.open(assetPath).use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return Uri.fromFile(cacheFile)
    }

    fun createDemoPlaylist(): Playlist = Playlist(
        id = "demo",
        title = "Road Trip Vibes",
        description = "A mix of upbeat and relaxing tunes for your next journey.",
        songs = emptyList(),
    )

    companion object {
        private const val ASSETS_MUSIC = "music"
        private val EXCLUDED_STORAGE_NAMES = setOf("emulated", "self")

        private val bundledTrackFiles = listOf(
            "01_creep.mp3",
            "02_flying.mp3",
            "03_hotel_california.mp3",
            "04_november_rain.mp3",
            "05_lonely_day.mp3",
            "06_nothing_else_matters.mp3",
            "07_lovers_rock.mp3",
            "1.mp3",
        )
    }
}
