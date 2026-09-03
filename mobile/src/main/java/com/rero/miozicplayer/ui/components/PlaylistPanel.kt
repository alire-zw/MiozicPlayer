package com.rero.miozicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rero.miozicplayer.data.BrowseFolder
import com.rero.miozicplayer.data.Song
import com.rero.miozicplayer.ui.theme.CarDimensions
import com.rero.miozicplayer.ui.theme.MiozicTheme

@Composable
fun PlaylistPanel(
    title: String,
    description: String,
    songs: List<Song>,
    currentIndex: Int,
    onSongClick: (Int) -> Unit,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    browseFolders: List<BrowseFolder> = emptyList(),
    browseOpenedFolderId: String? = null,
    onFolderClick: (String) -> Unit = {},
    onBrowseBack: () -> Unit = {},
) {
    val colors = MiozicTheme.colors
    val showingBrowseFolders = browseFolders.isNotEmpty() && browseOpenedFolderId == null

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(
                start = CarDimensions.playlistPaddingStart,
                top = CarDimensions.playlistPaddingVertical,
                end = CarDimensions.playlistPaddingEnd,
                bottom = CarDimensions.playlistPaddingVertical,
            ),
    ) {
        ThemeToggle(
            isDarkMode = isDarkMode,
            onToggle = onThemeToggle,
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (browseOpenedFolderId != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBrowseBack)
                    .padding(vertical = 4.dp),
            ) {
                MiozicIcon(
                    iconRes = MiozicIcons.Previous,
                    contentDescription = "Back",
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                LocaleText(
                    text = "Back to folders",
                    fontSize = CarDimensions.playlistDescSize,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        LocaleText(
            text = title,
            fontSize = CarDimensions.playlistTitleSize,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(6.dp))

        LocaleText(
            text = description,
            fontSize = CarDimensions.playlistDescSize,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(CarDimensions.songItemSpacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (showingBrowseFolders) {
                items(browseFolders, key = { it.id }) { folder ->
                    FolderListItem(
                        folder = folder,
                        onClick = { onFolderClick(folder.id) },
                    )
                }
            } else {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SongListItem(
                        song = song,
                        isActive = index == currentIndex,
                        onClick = { onSongClick(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderListItem(
    folder: BrowseFolder,
    onClick: () -> Unit,
) {
    val colors = MiozicTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = colors.shadow)
            .clip(shape)
            .background(colors.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CarDimensions.songItemArtSize)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.pillBackground),
        ) {
            MiozicIcon(
                iconRes = MiozicIcons.Browse,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            LocaleText(
                text = folder.title,
                fontSize = CarDimensions.songTitleSize,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            LocaleText(
                text = "${folder.songCount} songs",
                fontSize = CarDimensions.songArtistSize,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiozicTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.shadow(4.dp, shape, ambientColor = colors.shadow)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(if (isActive) colors.activeSongCard else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isActive) 12.dp else 4.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CarDimensions.songItemArtSize)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AlbumArt(
                song = song,
                cornerRadius = 12.dp,
                iconSize = if (isActive) 24.dp else 20.dp,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            LocaleText(
                text = song.title,
                fontSize = CarDimensions.songTitleSize,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            LocaleText(
                text = song.artist,
                fontSize = CarDimensions.songArtistSize,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
