/**
 * JixOne — Spotify / YouTube-Music-style playlist header.
 *
 * A soft gradient backdrop that fades into the page, a large rounded cover
 * beside the playlist info (label, bold title, description, author row,
 * song count), and a row of round action buttons led by the big primary
 * play button — the layout used by Spotify's and YouTube Music's playlist
 * pages. Every playlist screen in the app shares this header so the look
 * stays consistent.
 */

package com.metrolist.music.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R

/** Size of the cover square in the header. */
private val HeaderCoverSize = 156.dp

/** Corner radius of the cover. */
private val HeaderCoverRadius = 14.dp

/**
 * The Spotify/YouTube-Music-style playlist header.
 *
 * @param cover      the cover artwork; drawn inside a rounded, shadowed
 *                   square of [HeaderCoverSize] — use fillMaxSize-based
 *                   modifiers so it scales with the frame.
 * @param label      small overline above the title (e.g. "Playlist").
 * @param title      playlist name, bold, up to three lines.
 * @param metadata   the "50 songs • 3 hr 20 min" line.
 * @param description optional description, two lines then ellipsis.
 * @param authorRow  optional composable for the creator (avatar + name).
 * @param actions    the round action buttons; the big primary play button
 *                   should come first, followed by the secondary circles.
 */
@Composable
fun ModernPlaylistHeader(
    cover: @Composable BoxScope.() -> Unit,
    label: String?,
    title: String,
    metadata: String,
    description: String? = null,
    authorRow: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    // Captured outside drawBehind — the lambda is not a composable scope.
    val gradientTop = MaterialTheme.colorScheme.surfaceContainerHigh
    val gradientBottom = MaterialTheme.colorScheme.surface

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(gradientTop, gradientBottom),
                                startY = 0f,
                                endY = size.height * 1.15f,
                            ),
                    )
                }
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(HeaderCoverSize)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(HeaderCoverRadius),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        )
                        .clip(RoundedCornerShape(HeaderCoverRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                content = cover,
            )

            Spacer(Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (authorRow != null) {
                    Spacer(Modifier.height(8.dp))
                    authorRow()
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions()
        }
    }
}

/** The big primary play circle that leads the action row. */
@Composable
fun PlaylistPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        shadowElevation = 6.dp,
        modifier = modifier.size(64.dp),
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = stringResource(R.string.play),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

/** A secondary round action button (shuffle, like, menu, download…). */
@Composable
fun PlaylistIconButton(
    onClick: () -> Unit,
    iconRes: Int,
    contentDescription: String?,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.size(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
