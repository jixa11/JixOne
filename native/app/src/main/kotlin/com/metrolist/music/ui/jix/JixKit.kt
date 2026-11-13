/**
 * JixOne "Vivid" design kit.
 *
 * The building blocks of the JixOne look: saturated accent gradients, glowing
 * controls, layered glass panels and large artwork. Screens compose these
 * instead of the stock Material list/grid items so the app reads as one
 * product.
 */

package com.metrolist.music.ui.jix

import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

/**
 * The theme's vivid accent. Material's generated dark schemes use pale
 * pastel tones for primary/tertiary, which wash out as fills; the accent is
 * built from the theme's own seed color instead, so gradients stay saturated.
 */
class JixAccent(
    val start: Color,
    val end: Color,
) {
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))

    /** Black or white, whichever reads on the gradient. */
    val ink: Color
        get() {
            val lum = (start.luminance() + end.luminance()) / 2f
            return if (lum > 0.42f) Color.Black else Color.White
        }
}

fun jixAccentFor(seed: Color): JixAccent {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsv)
    // A neutral seed (Pure OLED) stays neutral: silver into graphite.
    if (hsv[1] < 0.12f) return JixAccent(seed, Color(0xFF8E8E98))
    val saturation = hsv[1].coerceAtLeast(0.62f)
    val value = hsv[2].coerceIn(0.78f, 1f)
    val start = Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], saturation, value)))
    // Yellows and golds drift into a sickly green if pushed forward, so they
    // warm toward orange instead; every other hue moves forward.
    val shift = if (hsv[0] in 28f..78f) -32f else 42f
    val end = Color(android.graphics.Color.HSVToColor(floatArrayOf((hsv[0] + shift + 360f) % 360f, saturation, value)))
    return JixAccent(start, end)
}

val LocalJixAccent = staticCompositionLocalOf { jixAccentFor(Color(0xFF7C5CFF)) }

/** The signature accent gradient. */
@Composable
fun jixAccentBrush(): Brush = LocalJixAccent.current.brush

/** A translucent panel with a faint gradient rim — the kit's "glass". */
@Composable
fun Modifier.jixGlass(shape: Shape): Modifier =
    this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
        .border(
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                    ),
                ),
            ),
            shape,
        )

/** A round, glowing, gradient-filled play button. */
@Composable
fun JixPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onColor: Boolean = false,
) {
    val accent = LocalJixAccent.current
    val glow = accent.start
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .shadow(elevation = 14.dp, shape = CircleShape, ambientColor = glow, spotColor = glow)
                .clip(CircleShape)
                .background(if (onColor) Brush.linearGradient(listOf(Color.White, Color.White)) else jixAccentBrush())
                .clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.play),
            contentDescription = null,
            tint = if (onColor) accent.start else accent.ink,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

/**
 * Section header: a gradient accent bar, a small uppercase eyebrow and a
 * heavy title, with an optional round thumbnail and a "more" chevron.
 */
@Composable
fun JixSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    thumbnailUrl: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 20.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(46.dp)
                        .border(2.dp, jixAccentBrush(), CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape),
            )
        } else {
            Box(
                Modifier
                    .width(5.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(jixAccentBrush()),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** An artist avatar inside a gradient ring, with the name underneath. */
@Composable
fun JixArtistBubble(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(size + 12.dp).padding(vertical = 4.dp),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(size)
                    .border(3.dp, jixAccentBrush(), CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The second line under a card title, by item type. */
private fun YTItem.jixSubtitle(): String? =
    when (this) {
        is SongItem -> artists.joinToString { it.name }
        is AlbumItem -> listOfNotNull(artists?.joinToString { it.name }, year?.toString()).joinToString(" • ")
        is PlaylistItem -> author?.name ?: songCountText
        is PodcastItem -> author?.name
        is EpisodeItem -> author?.name
        is ArtistItem -> null
        else -> null
    }?.takeIf { it.isNotBlank() }

/**
 * A shelf card: large rounded artwork with a play badge on playable items,
 * a highlighted rim while it is the current track, title and subtitle below.
 * Artists render as a ringed bubble instead.
 */
@Composable
fun JixMediaCard(
    item: YTItem,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 156.dp,
) {
    if (item is ArtistItem) {
        JixArtistBubble(
            name = item.title,
            imageUrl = item.thumbnail,
            modifier = modifier.padding(horizontal = 6.dp),
            size = 112.dp,
        )
        return
    }

    val shape = RoundedCornerShape(24.dp)
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.03f else 1f,
        animationSpec = spring(),
        label = "jixCardScale",
    )
    Column(
        modifier =
            modifier
                .padding(horizontal = 6.dp)
                .width(width)
                .scale(scale),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(
                        elevation = if (isActive) 16.dp else 6.dp,
                        shape = shape,
                        ambientColor = LocalJixAccent.current.start,
                        spotColor = LocalJixAccent.current.start,
                    )
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .then(
                        if (isActive) Modifier.border(2.5.dp, jixAccentBrush(), shape) else Modifier,
                    ),
        ) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Bottom scrim so the badge reads on any artwork.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
            )
            if (item is SongItem || item is EpisodeItem) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(jixAccentBrush()),
                ) {
                    Icon(
                        painter = painterResource(if (isActive && isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = LocalJixAccent.current.ink,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.jixSubtitle()?.let { subtitle ->
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * The Explore hero: a full-width gradient card with the listener's artists
 * overlapping on one side, a heavy title and a white play button.
 */
@Composable
fun JixHeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    artistImages: List<String?>,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    val ink = LocalJixAccent.current.ink
    val glow = LocalJixAccent.current.start
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .shadow(elevation = 20.dp, shape = shape, ambientColor = glow, spotColor = glow)
                .clip(shape)
                .background(jixAccentBrush())
                .clickable(onClick = onPlay),
    ) {
        // Soft light blobs give the flat gradient some depth.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-60).dp)
                .size(200.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 60.dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.14f)),
        )
        Column(Modifier.padding(22.dp)) {
            Text(
                text = eyebrow.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = ink.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = ink.copy(alpha = 0.88f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                    artistImages.take(4).forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .size(46.dp)
                                    .border(2.5.dp, Color.White, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                JixPlayButton(onClick = onPlay, size = 58.dp, onColor = true)
            }
        }
    }
}

/** A round translucent icon button for use over artwork or blurred grounds. */
@Composable
fun JixGlassIconButton(
    icon: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f))
                .border(1.dp, tint.copy(alpha = 0.18f), CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * A player transport button. [filled] gives it a soft disc (previous / next);
 * [active] fills it solid to show a toggle is on (shuffle / repeat).
 */
@Composable
fun JixTransportButton(
    icon: Int,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    filled: Boolean = false,
    active: Boolean = false,
    activeContainer: Color = tint,
    activeTint: Color = Color.Black,
    enabled: Boolean = true,
) {
    val container =
        when {
            active -> activeContainer
            filled -> tint.copy(alpha = 0.14f)
            else -> Color.Transparent
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(container)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint =
                when {
                    !enabled -> tint.copy(alpha = 0.35f)
                    active -> activeTint
                    filled -> tint
                    else -> tint.copy(alpha = 0.7f)
                },
            modifier = Modifier.size(iconSize),
        )
    }
}

/** A menu row: accent-tinted icon disc, bold label. For dropdowns and sheets. */
@Composable
fun JixMenuItem(
    icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The JixOne loading indicator: five accent bars rising and falling like a
 * level meter. Replaces the stock spinner wherever something is loading.
 */
@Composable
fun JixLoader(
    modifier: Modifier = Modifier,
    height: Dp = 34.dp,
) {
    val accent = LocalJixAccent.current
    val transition = rememberInfiniteTransition(label = "jixLoader")
    Row(
        horizontalArrangement = Arrangement.spacedBy(height * 0.12f),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(height),
    ) {
        repeat(5) { index ->
            val level by transition.animateFloat(
                initialValue = 0.28f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 110),
                    ),
                label = "jixLoaderBar$index",
            )
            Box(
                Modifier
                    .width(height * 0.14f)
                    .height(height * level)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(accent.start, accent.end))),
            )
        }
    }
}
