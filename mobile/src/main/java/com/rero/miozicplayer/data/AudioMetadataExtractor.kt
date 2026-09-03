package com.rero.miozicplayer.data

import android.media.MediaMetadataRetriever
import java.io.InputStream
import java.nio.charset.Charset

data class AudioMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
)

object AudioMetadataExtractor {

    fun fromRetriever(
        retriever: MediaMetadataRetriever,
        fileName: String,
        id3Fallback: Id3Tags? = null,
    ): AudioMetadata {
        val baseName = fileName.substringBeforeLast('.')
        val retrieverTitle = retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val retrieverArtist = retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?: retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            ?: retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
        val retrieverAlbum = retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val duration = retriever.readMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L

        return AudioMetadata(
            title = pickBestLabel(
                baseName = baseName,
                retrieverValue = retrieverTitle,
                id3Value = id3Fallback?.title,
            ),
            artist = pickBestLabel(
                baseName = baseName,
                retrieverValue = retrieverArtist,
                id3Value = id3Fallback?.artist,
                unknownFallback = "Unknown Artist",
            ),
            album = pickBestLabel(
                baseName = baseName,
                retrieverValue = retrieverAlbum,
                id3Value = id3Fallback?.album,
                unknownFallback = "Unknown Album",
            ),
            durationMs = duration,
        )
    }

    fun readId3Tags(input: InputStream): Id3Tags? {
        return try {
            val header = input.readNBytes(10)
            if (header.size < 10 || !header.copyOfRange(0, 3).contentEquals("ID3".toByteArray())) {
                return null
            }
            val tagSize = unsyncsafeInt(header, 6)
            val tagData = input.readNBytes(tagSize.coerceAtMost(512 * 1024))
            if (tagData.isEmpty()) return null

            Id3Tags(
                title = readId3Frame(tagData, "TIT2"),
                artist = readId3Frame(tagData, "TPE1"),
                album = readId3Frame(tagData, "TALB"),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun MediaMetadataRetriever.readMetadata(key: Int): String? {
        return extractMetadata(key)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun pickBestLabel(
        baseName: String,
        retrieverValue: String?,
        id3Value: String?,
        unknownFallback: String? = null,
    ): String {
        val candidates = listOfNotNull(retrieverValue, id3Value)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        candidates.firstOrNull { !looksLikeFileName(it, baseName) }?.let { return it }
        candidates.firstOrNull()?.let { return it }
        unknownFallback?.let { return it }
        return baseName
    }

    private fun looksLikeFileName(value: String, baseName: String): Boolean {
        if (value.equals(baseName, ignoreCase = true)) return true
        if (baseName.matches(Regex("^\\d+$")) && value == baseName) return true
        return false
    }

    private fun unsyncsafeInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    private fun readId3Frame(tagData: ByteArray, frameId: String): String? {
        var index = 0
        while (index + 10 <= tagData.size) {
            val id = String(tagData, index, 4, Charsets.US_ASCII)
            if (id == "TXXX" || id.contains('\u0000') || !id[0].isLetter()) {
                break
            }
            val frameSize = readFrameSize(tagData, index + 4)
            if (frameSize <= 0 || index + 10 + frameSize > tagData.size) {
                break
            }
            if (id == frameId) {
                val framePayload = tagData.copyOfRange(index + 10, index + 10 + frameSize)
                decodeId3Text(framePayload)?.let { return it }
            }
            index += 10 + frameSize
        }
        return null
    }

    private fun readFrameSize(tagData: ByteArray, offset: Int): Int {
        return if (tagData[offset].toInt() and 0x80 == 0) {
            ((tagData[offset].toInt() and 0xFF) shl 24) or
                ((tagData[offset + 1].toInt() and 0xFF) shl 16) or
                ((tagData[offset + 2].toInt() and 0xFF) shl 8) or
                (tagData[offset + 3].toInt() and 0xFF)
        } else {
            unsyncsafeInt(tagData, offset)
        }
    }

    private fun decodeId3Text(payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        val encoding = payload[0].toInt()
        val textBytes = payload.copyOfRange(1, payload.size)
        val decoded = when (encoding) {
            0 -> textBytes.decodeTerminated(Charset.forName("ISO-8859-1"))
            1 -> textBytes.decodeUtf16WithBom()
            2 -> textBytes.decodeTerminated(Charsets.UTF_16BE)
            3 -> textBytes.decodeTerminated(Charsets.UTF_8)
            else -> textBytes.decodeUtf16WithBom()
                ?: textBytes.decodeTerminated(Charsets.UTF_8)
        }?.trim()?.takeIf { it.isNotBlank() }
        return decoded
    }

    private fun ByteArray.decodeUtf16WithBom(): String? {
        if (size < 2) return null
        return when {
            this[0] == 0xFF.toByte() && this[1] == 0xFE.toByte() ->
                String(copyOfRange(2, size), Charsets.UTF_16LE).trimEnd('\u0000')
            this[0] == 0xFE.toByte() && this[1] == 0xFF.toByte() ->
                String(copyOfRange(2, size), Charsets.UTF_16BE).trimEnd('\u0000')
            else -> String(this, Charsets.UTF_16BE).trimEnd('\u0000')
        }.trim().takeIf { it.isNotBlank() }
    }

    private fun ByteArray.decodeTerminated(charset: Charset): String? {
        val terminatorIndex = indexOfSequence(
            if (charset == Charsets.UTF_16BE || charset == Charsets.UTF_16LE) byteArrayOf(0, 0) else byteArrayOf(0),
        )
        val end = if (terminatorIndex >= 0) terminatorIndex else size
        if (end <= 0) return null
        return String(copyOfRange(0, end), charset).trim().takeIf { it.isNotBlank() }
    }

    private fun ByteArray.indexOfSequence(sequence: ByteArray): Int {
        if (sequence.isEmpty() || size < sequence.size) return -1
        for (i in 0..size - sequence.size) {
            if (copyOfRange(i, i + sequence.size).contentEquals(sequence)) return i
        }
        return -1
    }
}

data class Id3Tags(
    val title: String?,
    val artist: String?,
    val album: String?,
)

private fun InputStream.readNBytes(count: Int): ByteArray {
    val buffer = ByteArray(count)
    var offset = 0
    while (offset < count) {
        val read = read(buffer, offset, count - offset)
        if (read == -1) break
        offset += read
    }
    return if (offset == count) buffer else buffer.copyOf(offset)
}
