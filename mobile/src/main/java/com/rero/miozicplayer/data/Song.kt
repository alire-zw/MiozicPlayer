package com.rero.miozicplayer.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumArtUri: Uri? = null,
    val source: MusicSource = MusicSource.LOCAL,
)

enum class MusicSource {
    LOCAL,
    USB,
    BLUETOOTH,
    BUNDLED,
}

data class Playlist(
    val id: String,
    val title: String,
    val description: String,
    val songs: List<Song>,
)

data class BrowseFolder(
    val id: String,
    val title: String,
    val description: String,
    val songs: List<Song>,
) {
    val songCount: Int get() = songs.size
}

enum class NavDestination {
    BLUETOOTH,
    BROWSE,
}
