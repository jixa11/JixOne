/**
 * JixOne v6.1 — immersive playlist header.
 *
 * A full-bleed cover-tinted backdrop (the artwork, oversized and blurred,
 * washed into the page surface), a tinted label chip, oversized bold title,
 * and a gradient play pill — the Spotify-style hero shared by every
 * playlist screen in the app (Online / Local / Auto / Top 50 / Cache).
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R

/** Size of the cover square in the header. */
private val HeaderCoverSize = 158.dp

/** Corner radius of the cover. */
private val HeaderCoverRadius = 20.dp

/**
 * The immersive playlist header.
 *
 * @param backdropModel artwork model (thumbnail url) painted oversized and
 *   blurred behind the whole header; null falls back to a plain theme
 *   gradient.
 * @param cover      the cover artwork; drawn inside a rounded, shadowed
 *                   square of [HeaderCoverSize] — use fillMaxSize-based
 *                   modifiers so it scales with the frame.
 * @param label      small chip above the title (e.g. "Playlist").
 * @param title      playlist name, bold, up to three lines.
 * @param metadata   the "50 songs • 3 hr 20 min" line.
 * @param description optional description, two lines then ellipsis.
 * @param authorRow  optional composable for the creator (avatar + name).
 * @param actions    the round action buttons; the gradient play pill
 *                   should come first, followed by the secondary circles.
 */
@Composable
fun ModernPlaylistHeader(
    backdropModel: Any?,
    cover: @Composable BoxScope.() -> Unit,
    label: String?,
    title: String,
    metadata: String,
    description: String? = null,
    authorRow: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrimTop = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)
    val scrimBottom = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    // The page always lands on the surface color.
                    drawRect(brush = Brush.verticalGradient(listOf(scrimTop, scrimBottom), startY = 0f, endY = size.height * 1.25f))
                },
    ) {
        // Full-bleed blurred artwork backdrop.
        if (backdropModel != null) {
            AsyncImage(
                model = backdropModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = 1.35f
                            scaleY = 1.35f
                        }
                        .blur(52.dp),
            )
            // Theme-tint wash so the artwork matches the active palette.
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(tint),
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(HeaderCoverSize)
                            .shadow(
                                elevation = 22.dp,
                                shape = RoundedCornerShape(HeaderCoverRadius),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                            )
                            .clip(RoundedCornerShape(HeaderCoverRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    content = cover,
                )

                Spacer(Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (label != null) {
                        // Label chip — a tinted pill instead of a bare overline.
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
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

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }
    }
}

/** The big gradient play pill that leads the action row. */
@Composable
fun PlaylistPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = MaterialTheme.colorScheme.primary
    val end = MaterialTheme.colorScheme.tertiary
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = CircleShape,
        shadowElevation = 10.dp,
        modifier =
            modifier
                .size(66.dp)
                .background(
                    brush = Brush.linearGradient(listOf(start, end)),
                    shape = CircleShape,
                ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // Soft inner glow.
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            ),
                            shape = CircleShape,
                        ),
            )
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = stringResource(R.string.play),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
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
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            ),
        modifier = modifier.size(48.dp),
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
