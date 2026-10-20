/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.R
import com.metrolist.music.ui.utils.resize

/** Width of a compact shelf card — the YTM/Spotify shelf rhythm. */
val JixShelfCardWidth = 152.dp

/** The gap every shelf card carries on its end, so rows need no spacing setup. */
val JixShelfGap = 10.dp

/**
 * JixOne modern section header for shelf rows: a bold compact title with an
 * optional label above it and an optional "see all" pill on the end.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JixShelfHeader(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onClick != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .combinedClickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.see_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        painter = painterResource(R.drawable.jix_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/**
 * The compact modern shelf card — square rounded artwork, tight 2-line title,
 * subtle subtitle, tiny badges, and an equalizer chip on the artwork when
 * this exact item is the one playing. Sized like YT Music's shelf tiles,
 * a third of the old oversized grid cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JixShelfCard(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    width: Dp = JixShelfCardWidth,
    isCircle: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val shape = if (isCircle) CircleShape else RoundedCornerShape(12.dp)

    // the card carries its own trailing gap so every shelf row (with or
    // without Arrangement.spacedBy) gets an even, modern rhythm
    Box(modifier = modifier.width(width + JixShelfGap)) {
        Column(
            modifier = Modifier
                .width(width)
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick?.invoke()
                    },
                ),
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(width)
        ) {
            AsyncImage(
                model = thumbnailUrl?.resize(400, 400),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(width)
                    .clip(shape),
            )

            // playing chip on the artwork — the modern "this is playing" cue
            if (isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPlaying) {
                            PlayingIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                bars = 3,
                                barWidth = 2.dp,
                                cornerRadius = 1.dp,
                                modifier = Modifier.height(10.dp),
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.jix_play),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
            }

            // badges (explicit / liked / downloaded) as a subtle overlay row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp),
            ) {
                badges()
            }
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
        }
    }
}

/**
 * Convenience wrapper that derives the subtitle + circle shape from a YTItem,
 * so screens can render a whole shelf with one call.
 */
@Composable
fun JixShelfItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val subtitle = when (item) {
        is SongItem -> item.artists.joinToString(", ") { it.name }
        is AlbumItem -> item.artists?.firstOrNull()?.name
            ?: item.year?.toString()
        is PlaylistItem -> item.author?.name ?: item.songCountText
        is PodcastItem -> item.author?.name ?: item.episodeCountText
        is EpisodeItem -> item.publishDateText ?: item.author?.name
        is ArtistItem -> null
        else -> null
    }
    JixShelfCard(
        title = item.title,
        subtitle = subtitle,
        thumbnailUrl = item.thumbnail,
        isCircle = item is ArtistItem,
        isActive = isActive,
        isPlaying = isPlaying,
        badges = badges,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

/** Shared LazyRow shelf arrangement: start-aligned, 12dp screen padding, 10dp gaps. */
val JixShelfRowPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
val JixShelfRowSpacing = 10.dp
