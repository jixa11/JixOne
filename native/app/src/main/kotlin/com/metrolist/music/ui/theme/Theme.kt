/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

// JixOne brand default: the neon blue-violet of the J-Note app icon.
// Kept identical everywhere via this constant; selecting "Dynamic" in the
// theme picker stores this value as the "follow system wallpaper" sentinel.
val DefaultThemeColor = Color(0xFF5F5BF9)

// How much the artwork veil covers: high enough that text on top keeps full
// contrast (same idea as the web app's veil), low enough the art still reads.
private const val ArtworkVeilAlphaDark = 0.87f
private const val ArtworkVeilAlphaLight = 0.82f

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    showArtwork: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Persian artwork theme (JixOne): web-app backgrounds + full-bleed art
    val persianTheme = if (showArtwork) persianThemeFor(themeColor) else null
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = themeColor, // themeColor is guaranteed non-default here
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot // Keep existing style
        )
    }

    // Apply pureBlack modification if needed, similar to original logic.
    // When a Persian artwork theme is active the background becomes
    // transparent so the artwork layer (drawn at the app root) shows through;
    // every other surface stays opaque, so content remains fully readable.
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme, persianTheme) {
        var scheme =
            if (darkTheme && pureBlack) {
                baseColorScheme.pureBlack(true)
            } else {
                baseColorScheme
            }
        if (persianTheme != null) {
            scheme = scheme.copy(background = Color.Transparent)
        }
        scheme
    }

    // Veil over the artwork, colored with the theme's own web-app background.
    val artwork = persianTheme?.let { theme ->
        val veilBase = if (darkTheme) {
            theme.darkBackground ?: baseColorScheme.background
        } else {
            theme.lightBackground ?: baseColorScheme.background
        }
        ThemeArtwork(
            artworkRes = theme.artworkRes,
            tiled = theme.tiled,
            veilColor = veilBase.copy(
                alpha = if (darkTheme) ArtworkVeilAlphaDark else ArtworkVeilAlphaLight
            ),
        )
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    CompositionLocalProvider(LocalThemeArtwork provides artwork) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
