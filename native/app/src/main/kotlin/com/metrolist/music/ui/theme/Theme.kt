/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

// JixOne brand default: the neon blue-violet of the J-Note app icon.
// Kept identical everywhere via this constant; selecting "Dynamic" in the
// theme picker stores this value as the "follow system wallpaper" sentinel.
val DefaultThemeColor = Color(0xFF5F5BF9)

/**
 * JixOne modern shape language: generous, soft corners everywhere —
 * the visual signature that separates JixOne from stock Material/Metrolist.
 */
val JixOneShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * JixOne gradient kit — used by the player screen, cards and headers to give
 * the app a modern layered look instead of flat minimal surfaces.
 */
object JixOneGradients {
    /** Top-of-screen tint that fades into the background (player, headers). */
    @Composable
    fun scrim(top: Color, bottom: Color): Brush = Brush.verticalGradient(
        0.0f to top,
        0.55f to bottom.copy(alpha = 0.55f),
        1.0f to bottom,
    )

    /** Accent gradient for highlighted cards / chips / progress pills. */
    @Composable
    fun accent(): Brush {
        val c = MaterialTheme.colorScheme
        return Brush.linearGradient(listOf(c.primary, c.tertiary))
    }
}

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
    // Veil strength per theme, ported 1:1 from the web app's globals.css
    // (art opacity .30 carpet / .34 tile / .32 derafsh / .20 cyrus) so the
    // artwork is exactly as visible as it was on the web.
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
                alpha = if (darkTheme) theme.veilAlphaDark else theme.veilAlphaLight
            ),
        )
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    CompositionLocalProvider(LocalThemeArtwork provides artwork) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = JixOneShapes,
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
