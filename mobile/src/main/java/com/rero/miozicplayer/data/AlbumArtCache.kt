package com.rero.miozicplayer.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

object AlbumArtCache {

    fun extractFromRetriever(context: Context, retriever: MediaMetadataRetriever, cacheKey: String): Uri? {
        val bytes = retriever.embeddedPicture ?: return null
        return save(context, cacheKey, bytes)
    }

    fun save(context: Context, cacheKey: String, bytes: ByteArray): Uri? {
        val safeKey = cacheKey.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dir = File(context.cacheDir, "album_art")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$safeKey.jpg")
        return try {
            if (!file.exists() || file.length() != bytes.size.toLong()) {
                file.writeBytes(bytes)
            }
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }
}
