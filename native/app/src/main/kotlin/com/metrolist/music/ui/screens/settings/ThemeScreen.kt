package com.metrolist.music.ui.screens.settings

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.PureBlackMiniPlayerKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.JixOneThemes
import com.metrolist.music.ui.theme.MetrolistTheme
import com.metrolist.music.ui.theme.PersianCarpetSeed
import com.metrolist.music.ui.theme.jixOneThemeFor
import com.metrolist.music.ui.theme.persianThemeFor
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

data class ThemePalette(
    val nameRes: Int,
    val seedColor: Color
)

/**
 * The ONLY palettes the picker offers (JixOne 5.0): the web app's 12 themes
 * plus the Android wallpaper "Dynamic" sentinel. All 19 Metrolist seed
 * colors are gone for good.
 */
val PaletteColors = listOf(
    ThemePalette(R.string.palette_dynamic, Color.Transparent), // Sentinel for System/Dynamic colors
) + JixOneThemes.map { ThemePalette(it.nameRes, it.seed) }

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
    val (selectedThemeColorInt, onSelectedThemeColorChange) = rememberPreference(
        SelectedThemeColorKey,
        PersianCarpetSeed.toArgb()
    )
    val (_, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = false)

    val selectedThemeColor = Color(selectedThemeColorInt)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Helper function to handle color selection with dynamic theme toggle
    val handleColorSelection: (Color) -> Unit = { color ->
        onSelectedThemeColorChange(color.toArgb())
        // Enable dynamic theme only when selecting the default/dynamic color
        // Disable it when selecting any other color
        val isDynamicColor = color == DefaultThemeColor
        onDynamicThemeChange(isDynamicColor)
    }

    if (isLandscape) {
        LandscapeThemeLayout(
            innerPadding = PaddingValues(0.dp),
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
            pureBlack = pureBlack,
            onPureBlackChange = onPureBlackChange,
            selectedThemeColor = selectedThemeColor,
            onSelectedThemeColorChange = handleColorSelection
        )
    } else {
        PortraitThemeLayout(
            innerPadding = PaddingValues(0.dp),
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
            pureBlack = pureBlack,
            onPureBlackChange = onPureBlackChange,
            selectedThemeColor = selectedThemeColor,
            onSelectedThemeColorChange = handleColorSelection
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.theme_colors)) },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    painter = painterResource(R.drawable.jix_arrow_left),
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
        }
    )
}

@Composable
fun PortraitThemeLayout(
    innerPadding: PaddingValues,
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(84.dp))

        Box(
            modifier = Modifier
                .width(104.dp)
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            ThemeMockupPortrait(
                darkMode = darkMode,
                pureBlack = pureBlack,
                themeColor = selectedThemeColor
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        ThemeControls(
            darkMode = darkMode,
            onDarkModeChange = onDarkModeChange,
            pureBlack = pureBlack,
            onPureBlackChange = onPureBlackChange,
            selectedThemeColor = selectedThemeColor,
            onSelectedThemeColorChange = onSelectedThemeColorChange
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun LandscapeThemeLayout(
    innerPadding: PaddingValues,
    darkMode: DarkMode,
    onDarkModeChange: (DarkMode) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    selectedThemeColor: Color,
    onSelectedThemeColorChange: (Color) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .heightIn(max = 300.dp),
                contentAlignment = Alignment.Center
            ) {
                ThemeMockup(
                    darkMode = darkMode,
                    pureBlack = pureBlack,
                    themeColor = selectedThemeColor
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            ThemeControls(
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                pureBlack = pureBlack,
                onPureBlackChange = onPureBlackChange,
                selectedThemeColor = selectedThemeColor,
                onSelectedThemeColorChange = onSelectedThemeColorChange
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.color_palette),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // JixOne 5.0: the web app's picker — a 2-column grid of theme
                // cards (bg + artwork + swatch dots), replacing the Metrolist
                // swatch strip.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaletteColors.chunked(2).forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            rowItems.forEach { palette ->
                                val isDynamicPalette = palette.seedColor == Color.Transparent
                                val isSelected = if (isDynamicPalette) {
                                    selectedThemeColor == DefaultThemeColor
                                } else {
                                    selectedThemeColor == palette.seedColor
                                }

                                ThemeCard(
                                    palette = palette,
                                    isSelected = isSelected,
                                    onClick = {
                                        val colorToSave = if (isDynamicPalette) DefaultThemeColor else palette.seedColor
                                        onSelectedThemeColorChange(colorToSave)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
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
    
    // Use actual system colors for AUTO mode on Android 12+
    val modeColorScheme = if (targetMode == DarkMode.AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = DefaultThemeColor,
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
 * JixOne 5.0: web-style theme card (SettingsView.tsx port) — the theme's
 * real background color, Persian artwork peeking from the bottom with a
 * gradient mask, three swatch dots and a check badge when selected.
 */
@Composable
fun ThemeCard(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDynamic = palette.seedColor == Color.Transparent
    val def = jixOneThemeFor(palette.seedColor)
    val previewBg = when {
        isDynamic -> MaterialTheme.colorScheme.surfaceContainerHighest
        def != null && def.defaultsLight -> def.lightBg
        def != null -> def.darkBg
        else -> palette.seedColor
    }
    val onPreview = if (previewBg.luminance() > 0.5f) Color(0xFF18181B) else Color(0xFFFAFAFC)
    val accent = def?.accent ?: MaterialTheme.colorScheme.primary
    val accent2 = def?.accent2 ?: MaterialTheme.colorScheme.secondary
    val highlight = def?.highlight ?: MaterialTheme.colorScheme.tertiary
    val name = stringResource(palette.nameRes)
    val cardShape = RoundedCornerShape(20.dp)

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
        animationSpec = tween(180),
        label = "themeCardBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(180),
        label = "themeCardBorderWidth",
    )

    Box(
        modifier = modifier
            .height(92.dp)
            .clip(cardShape)
            .background(previewBg)
            .border(borderWidth, borderColor, cardShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = name },
    ) {
        // Persian artwork in the lower two-thirds, masked like the web picker
        if (def?.artworkRes != null) {
            Image(
                painter = painterResource(def.artworkRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .align(Alignment.BottomCenter),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(previewBg, previewBg.copy(alpha = 0.15f)),
                        )
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = onPreview,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isDynamic) {
                    Icon(
                        painter = painterResource(R.drawable.jix_palette),
                        contentDescription = null,
                        tint = onPreview.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .border(1.dp, onPreview.copy(alpha = 0.45f), CircleShape),
                    )
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(accent2),
                    )
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(highlight.copy(alpha = 0.8f)),
                    )
                }
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    tint = if (accent.luminance() > 0.45f) Color(0xFF101014) else Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
fun ThemeMockup(
    darkMode: DarkMode,
    pureBlack: Boolean,
    themeColor: Color
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = when (darkMode) {
        DarkMode.AUTO -> isSystemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }

    MetrolistTheme(
        darkTheme = useDark,
        pureBlack = pureBlack,
        themeColor = themeColor
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(9f / 18f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(6.dp))
                        )
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(6.dp))
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeMockupPortrait(
    darkMode: DarkMode,
    pureBlack: Boolean,
    themeColor: Color
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDark = when (darkMode) {
        DarkMode.AUTO -> isSystemDark
        DarkMode.ON -> true
        DarkMode.OFF -> false
    }

    MetrolistTheme(
        darkTheme = useDark,
        pureBlack = pureBlack,
        themeColor = themeColor
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header (20% of height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.2f)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        )
                    }
                }

                // Main Content (60% of height)
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                        )
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(4.dp))
                        )
                    }
                }

                // FAB Area (20% of height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.2f)
                        .padding(6.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    )
                }
            }
        }
    }
}
