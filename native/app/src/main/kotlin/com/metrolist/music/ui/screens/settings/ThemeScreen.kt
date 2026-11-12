package com.metrolist.music.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.PureBlackMiniPlayerKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.ui.theme.DefaultPaletteSeed
import com.metrolist.music.ui.theme.persianThemeFor
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

/**
 * A JixOne theme is a complete look: its color seed, whether it is dark or
 * light, and whether it uses a pure-black ground. Picking one sets all three,
 * so there is nothing else to configure.
 */
data class ThemePalette(
    val nameRes: Int,
    val seedColor: Color,
    val dark: Boolean = true,
    val pureBlack: Boolean = false,
)

val PaletteColors = listOf(
    ThemePalette(R.string.palette_aurora, Color(0xFF7C5CFF)),
    ThemePalette(R.string.palette_jixone_carpet, Color(0xFFD9A441)),
    ThemePalette(R.string.palette_jixone_tile, Color(0xFF33C5D6)),
    ThemePalette(R.string.palette_jixone_derafsh, Color(0xFF9A5BE0)),
    // Cyrus is the one light theme: gold and lapis on parchment.
    ThemePalette(R.string.palette_jixone_cyrus, Color(0xFF2A5D9F), dark = false),
    ThemePalette(R.string.palette_neon_crimson, Color(0xFFFF2D55)),
    ThemePalette(R.string.palette_neo_tokyo, Color(0xFF22D3EE)),
    ThemePalette(R.string.palette_synthwave, Color(0xFFFF2FA0)),
    ThemePalette(R.string.palette_classic_spotify, Color(0xFF1DB954)),
    ThemePalette(R.string.palette_pure_oled, Color(0xFFE4E4E7), pureBlack = true),
    ThemePalette(R.string.palette_dubai_sunset, Color(0xFFFF6B35)),
    ThemePalette(R.string.palette_youtube, Color(0xFFFF0033)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    navController: NavController,
) {
    val (_, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.ON)
    val (_, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (_, onPureBlackMiniPlayerChange) = rememberPreference(PureBlackMiniPlayerKey, defaultValue = false)
    val (selectedSeed, onSelectedSeedChange) =
        rememberPreference(SelectedThemeColorKey, DefaultPaletteSeed.toArgb())

    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding =
            PaddingValues(
                start = insets.calculateStartPadding(layoutDirection) + 18.dp,
                end = insets.calculateEndPadding(layoutDirection) + 18.dp,
                top = insets.calculateTopPadding() + 12.dp,
                bottom = insets.calculateBottomPadding() + 24.dp,
            ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.jix_theme_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(PaletteColors, key = { it.nameRes }) { palette ->
            ThemeCard(
                palette = palette,
                selected = selectedSeed == palette.seedColor.toArgb(),
                onClick = {
                    onSelectedSeedChange(palette.seedColor.toArgb())
                    onDarkModeChange(if (palette.dark) DarkMode.ON else DarkMode.OFF)
                    onPureBlackChange(palette.pureBlack)
                    onPureBlackMiniPlayerChange(palette.pureBlack)
                },
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.theme_colors)) },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
    )
}

/** A miniature of the app in this theme, with its name underneath. */
@Composable
private fun ThemeCard(
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // The same generator the real theme uses, so the preview shows the real colors.
    val scheme =
        rememberDynamicColorScheme(
            seedColor = palette.seedColor,
            isDark = palette.dark,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.Expressive,
        )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = spring(),
        label = "themeCardScale",
    )
    val shape = RoundedCornerShape(26.dp)
    val jixAccent = com.metrolist.music.ui.jix.jixAccentFor(palette.seedColor)
    val accent = jixAccent.brush

    Column(
        modifier =
            Modifier
                .scale(scale)
                .clip(shape)
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.62f)
                    .clip(shape)
                    .border(
                        BorderStroke(
                            if (selected) 3.dp else 1.dp,
                            if (selected) {
                                accent
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.outlineVariant,
                                        MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                )
                            },
                        ),
                        shape,
                    ),
        ) {
            ThemePreviewGround(palette = palette, scheme = scheme)
            ThemePreviewContent(scheme = scheme, accent = accent)

            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accent),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = jixAccent.ink,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Text(
            text = stringResource(palette.nameRes),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

/** The theme's background: its artwork under a veil, or its color wash. */
@Composable
private fun ThemePreviewGround(
    palette: ThemePalette,
    scheme: ColorScheme,
) {
    val artwork = persianThemeFor(palette.seedColor)
    val base =
        when {
            palette.pureBlack -> Color.Black
            artwork != null && palette.dark -> artwork.darkBackground ?: scheme.surface
            artwork != null -> artwork.lightBackground ?: scheme.surface
            else -> scheme.surface
        }
    Box(Modifier.fillMaxSize().background(base)) {
        if (artwork != null) {
            Image(
                painter = painterResource(artwork.artworkRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        base.copy(alpha = if (palette.dark) artwork.veilAlphaDark else artwork.veilAlphaLight),
                    ),
            )
        } else if (!palette.pureBlack) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(scheme.primary.copy(alpha = 0.38f), Color.Transparent),
                            radius = 420f,
                            center = androidx.compose.ui.geometry.Offset(380f, 0f),
                        ),
                    ),
            )
        }
    }
}

/** The mock screen: greeting, hero card, artist bubbles, two cards, the dock. */
@Composable
private fun ThemePreviewContent(
    scheme: ColorScheme,
    accent: Brush,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        PreviewBar(color = scheme.primary, widthFraction = 0.3f, height = 5)
        Spacer(Modifier.height(5.dp))
        PreviewBar(color = scheme.onSurface, widthFraction = 0.55f, height = 9)
        Spacer(Modifier.height(10.dp))

        // hero
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1.25f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent),
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            Column(Modifier.padding(9.dp)) {
                PreviewBar(color = Color.White.copy(alpha = 0.7f), widthFraction = 0.3f, height = 4)
                Spacer(Modifier.height(4.dp))
                PreviewBar(color = Color.White, widthFraction = 0.5f, height = 8)
            }
        }
        Spacer(Modifier.height(10.dp))

        // artist bubbles
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(4) {
                Box(
                    Modifier
                        .size(24.dp)
                        .border(2.dp, accent, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHighest),
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            repeat(2) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (index == 0) {
                                lerp(scheme.surfaceContainerHigh, scheme.primary, 0.25f)
                            } else {
                                lerp(scheme.surfaceContainerHigh, scheme.tertiary, 0.25f)
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // dock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(scheme.surfaceContainerHigh)
                    .border(1.dp, scheme.primary.copy(alpha = 0.4f), RoundedCornerShape(13.dp)),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(scheme.onSurfaceVariant.copy(alpha = 0.6f)))
            Box(Modifier.width(26.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(accent))
            Box(Modifier.size(8.dp).clip(CircleShape).background(scheme.onSurfaceVariant.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun PreviewBar(
    color: Color,
    widthFraction: Float,
    height: Int,
) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}
