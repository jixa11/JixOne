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
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R

/** Size of the cover square in the header. */
private val HeaderCoverSize = 204.dp

/** Corner radius of the cover. */
private val HeaderCoverRadius = 30.dp

/**
 * The playlist hero: the artwork blurred behind a dark scrim, the cover
 * centered and glowing, then label, title, author, metadata and the action
 * row. The scrim makes the ground dark whatever the artwork or app theme is,
 * so everything inside is drawn light — the title can never disappear into
 * the backdrop.
 *
 * @param backdropModel artwork model (thumbnail url) blurred behind the
 *   header; null falls back to the accent gradient.
 * @param cover      the cover artwork, drawn inside a rounded square of
 *                   [HeaderCoverSize] — use fillMaxSize-based modifiers.
 * @param label      small chip above the title (e.g. "Playlist").
 * @param title      playlist name, heavy, up to two lines.
 * @param metadata   the "50 songs • 3 hr 20 min" line.
 * @param description optional description, two lines then ellipsis.
 * @param authorRow  optional composable for the creator (avatar + name).
 * @param actions    the round action buttons; the play button first.
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
    val base = MaterialTheme.colorScheme
    // A light scheme's primary is too dark to read on the scrim.
    val lightScheme = base.surface.luminance() > 0.5f
    val accent = if (lightScheme) base.inversePrimary else base.primary
    val heroShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(heroShape)
                .background(com.metrolist.music.ui.jix.LocalJixAccent.current.brush),
    ) {
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
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.38f), Color.Black.copy(alpha = 0.78f)),
                        ),
                    ),
        )

        // Everything in the hero sits on the dark scrim: hand the content a
        // scheme whose text and control colors are light.
        MaterialTheme(
            colorScheme =
                base.copy(
                    primary = accent,
                    tertiary = if (lightScheme) base.tertiaryContainer else base.tertiary,
                    onPrimary = Color.Black,
                    onSurface = Color.White,
                    onSurfaceVariant = Color.White.copy(alpha = 0.82f),
                    surface = Color.White.copy(alpha = 0.16f),
                    surfaceVariant = Color.White.copy(alpha = 0.16f),
                    outlineVariant = Color.White.copy(alpha = 0.30f),
                    error = Color(0xFFFF4D6D),
                ),
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(HeaderCoverSize)
                                .shadow(
                                    elevation = 28.dp,
                                    shape = RoundedCornerShape(HeaderCoverRadius),
                                    ambientColor = accent,
                                    spotColor = accent,
                                )
                                .clip(RoundedCornerShape(HeaderCoverRadius))
                                .background(Color.White.copy(alpha = 0.12f)),
                        content = cover,
                    )

                    Spacer(Modifier.height(18.dp))

                    if (label != null) {
                        Text(
                            text = label.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (authorRow != null) {
                        Spacer(Modifier.height(10.dp))
                        authorRow()
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )

                    if (!description.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.70f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}

/** The glowing gradient play button that leads the action row. */
@Composable
fun PlaylistPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val jixAccent = com.metrolist.music.ui.jix.LocalJixAccent.current
    val start = jixAccent.start
    val end = jixAccent.end
    val ink = jixAccent.ink
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(70.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, ambientColor = start, spotColor = start)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(start, end)))
                .clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.play),
            contentDescription = stringResource(R.string.play),
            tint = ink,
            modifier = Modifier.size(32.dp),
        )
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
