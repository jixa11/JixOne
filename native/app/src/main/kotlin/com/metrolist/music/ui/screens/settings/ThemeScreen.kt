package com.metrolist.music.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.PureBlackMiniPlayerKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.ui.theme.DefaultPaletteSeed
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

data class ThemePalette(
    val nameRes: Int,
    val seedColor: Color
)

//
// JixOne 6: exactly twelve curated palettes — the JixOne identity set.
// The pre-6 "Dynamic / system wallpaper" entry and the extra "YouTube
// Light" variant are gone; every palette is a fixed, hand-tuned seed.
//
val PaletteColors = listOf(
    ThemePalette(R.string.palette_aurora, Color(0xFF7C5CFF)), // JixOne 5.1: Aurora — the app default
    ThemePalette(R.string.palette_jixone_carpet, Color(0xFFD9A441)), // JixOne: Persian carpet gold
    ThemePalette(R.string.palette_jixone_tile, Color(0xFF33C5D6)), // JixOne: Isfahan tile turquoise
    ThemePalette(R.string.palette_jixone_derafsh, Color(0xFF9A5BE0)), // JixOne: Derafsh Kaviani violet
    ThemePalette(R.string.palette_jixone_cyrus, Color(0xFF2A5D9F)), // JixOne: Cyrus blue
    ThemePalette(R.string.palette_neon_crimson, Color(0xFFFF2D55)), // JixOne web: Neon Crimson
    ThemePalette(R.string.palette_neo_tokyo, Color(0xFF22D3EE)), // JixOne web: Neo Tokyo
    ThemePalette(R.string.palette_synthwave, Color(0xFFFF2FA0)), // JixOne web: Synthwave
    ThemePalette(R.string.palette_classic_spotify, Color(0xFF1DB954)), // JixOne web: Classic Spotify
    ThemePalette(R.string.palette_pure_oled, Color(0xFFE4E4E7)), // JixOne web: Pure OLED (neutral)
    ThemePalette(R.string.palette_dubai_sunset, Color(0xFFFF6B35)), // JixOne web: Dubai Sunset
    ThemePalette(R.string.palette_youtube, Color(0xFFFF0033)), // JixOne web: YouTube
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    navController: NavController,
) {
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (pureBlack, onPureBlackChangeRaw) = rememberPreference(PureBlackKey, defaultValue = false)
    val (_, onPureBlackMiniPlayerChange) = rememberPreference(
        PureBlackMiniPlayerKey,
        defaultValue = false
    )

    val onPureBlackChange: (Boolean) -> Unit = { enabled ->
        onPureBlackChangeRaw(enabled)
        onPureBlackMiniPlayerChange(enabled)
    }
    val (selectedThemeColorInt, onSelectedThemeColorChangeInt) = rememberPreference(
        SelectedThemeColorKey,
        DefaultPaletteSeed.toArgb()
    )
    val onSelectedThemeColorChange: (Color) -> Unit = { color ->
        onSelectedThemeColorChangeInt(color.toArgb())
    }

    val selectedThemeColor = Color(selectedThemeColorInt)

    // JixOne 6: one simple scrollable layout for every orientation — the
    // phone mockup preview is gone, the palette circles are the preview.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        ThemeControls(
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
            pureBlack = pureBlack,
            onPureBlackChange = onPureBlackChange,
            selectedThemeColor = selectedThemeColor,
            onSelectedThemeColorChange = onSelectedThemeColorChange
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.theme_colors)) },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
        }
    )
}

@Composable
fun ThemeControls(
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Mode ────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.theme_mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System mode (AUTO)
                    ModeCircle(
                        darkMode = darkMode,
                        pureBlack = pureBlack,
                        targetMode = DarkMode.AUTO,
                        targetPureBlack = pureBlack,
                        onClick = {
                            onDarkModeChange(DarkMode.AUTO)
                        },
                        showIcon = true
                    )

                    // Vertical divider to separate System from manual modes
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Manual modes (Light, Dark, Pure Black)
                    ModeCircle(
                        darkMode = darkMode,
                        pureBlack = pureBlack,
                        targetMode = DarkMode.OFF,
                        targetPureBlack = false,
                        onClick = {
                            onDarkModeChange(DarkMode.OFF)
                            onPureBlackChange(false)
                        },
                        showIcon = false
                    )

                    ModeCircle(
                        darkMode = darkMode,
                        pureBlack = pureBlack,
                        targetMode = DarkMode.ON,
                        targetPureBlack = false,
                        onClick = {
                            onDarkModeChange(DarkMode.ON)
                            onPureBlackChange(false)
                        },
                        showIcon = false
                    )

                    ModeCircle(
                        darkMode = darkMode,
                        pureBlack = pureBlack,
                        targetMode = DarkMode.ON,
                        targetPureBlack = true,
                        onClick = {
                            onDarkModeChange(DarkMode.ON)
                            onPureBlackChange(true)
                        },
                        showIcon = false
                    )
                }
            }

            // ── Palette ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.color_palette),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(PaletteColors) { palette ->
                        PaletteItem(
                            palette = palette,
                            isSelected = selectedThemeColor == palette.seedColor,
                            onClick = { onSelectedThemeColorChange(palette.seedColor) }
                        )
                    }
                }

                // The active palette's name — instant feedback for the circles.
                val selectedName =
                    PaletteColors.firstOrNull { it.seedColor == selectedThemeColor }?.nameRes
                Text(
                    text = selectedName?.let { stringResource(it) }
                        ?: stringResource(R.string.palette_aurora),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ModeCircle(
    darkMode: DarkMode,
    pureBlack: Boolean,
    targetMode: DarkMode,
    targetPureBlack: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val isSelected = darkMode == targetMode && pureBlack == targetPureBlack

    val effectiveDark = when (targetMode) {
        DarkMode.AUTO -> isSystemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }

    // Preview colors for the circle fill: system scheme for AUTO on Android
    // 12+, otherwise the Aurora seed so the preview matches the app default.
    val modeColorScheme = if (targetMode == DarkMode.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = DefaultPaletteSeed,
            isDark = effectiveDark,
            style = PaletteStyle.TonalSpot
        )
    }

    val fillColor = when {
        targetPureBlack -> Color.Black
        effectiveDark -> modeColorScheme.surface
        else -> modeColorScheme.surface
    }

    // Animated border width
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderWidth"
    )

    // Animated scale for the entire circle
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val interactionSource = remember { MutableInteractionSource() }

    val contentDesc = when {
        targetPureBlack -> stringResource(R.string.cd_pure_black_mode)
        targetMode == DarkMode.OFF -> stringResource(R.string.cd_light_mode)
        targetMode == DarkMode.ON -> stringResource(R.string.cd_dark_mode)
        else -> stringResource(R.string.cd_system_mode)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(fillColor)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        width = borderWidth,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDesc
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            showIcon -> {
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = null,
                    tint = modeColorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            isSelected -> {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.3f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(
                        targetScale = 0.3f,
                        animationSpec = tween(150)
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inversePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * JixOne 6: a palette is a plain color circle — a soft vertical gradient of
 * the seed with the palette's own tint, a white check when selected. No
 * artwork thumbnails, no mockup: the circle IS the preview.
 */
@Composable
fun PaletteItem(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderWidth"
    )

    val interactionSource = remember { MutableInteractionSource() }

    val paletteName = stringResource(palette.nameRes)
    val contentDesc = stringResource(R.string.cd_palette_item, paletteName)

    // Deep end of the gradient: the seed shifted toward black keeps every
    // palette readable as a circle, even the light OLED neutral.
    val deep = lerp(palette.seedColor, Color.Black, 0.35f)

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(listOf(palette.seedColor, deep))
            )
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        width = borderWidth,
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDesc
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.3f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(
                targetScale = 0.3f,
                animationSpec = tween(150)
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
