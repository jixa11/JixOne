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
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

/**
 * JixOne type scale — same M3 sizes and line heights (so no layout shifts),
 * but with the web app's bolder hierarchy: headings carry real weight, so
 * screens read "designed" instead of default Material. Body text stays at the
 * M3 default for comfortable reading (incl. Persian).
 */
val JixOneTypography: Typography = run {
    val m3 = Typography()
    Typography(
        displayLarge = m3.displayLarge.copy(fontWeight = FontWeight.Black),
        displayMedium = m3.displayMedium.copy(fontWeight = FontWeight.Black),
        displaySmall = m3.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
        headlineLarge = m3.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = m3.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = m3.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = m3.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = m3.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = m3.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = m3.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = m3.labelMedium.copy(fontWeight = FontWeight.Medium),
        labelSmall = m3.labelSmall.copy(fontWeight = FontWeight.Medium),
        // bodyLarge / bodyMedium / bodySmall: unchanged M3 defaults
    )
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
    // JixOne 5.0: one of the 12 web themes → hand-tuned scheme, not tonal-spot
    val jixOneDef = jixOneThemeFor(themeColor)
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (jixOneDef != null) {
        // The web app's own palette: surfaces from the theme's real background
        // tone, accents from --accent / --accent-2 / mesh highlight. This is
        // what kills the "Metrolist with a recolor" feel.
        remember(jixOneDef, darkTheme) { buildJixOneColorScheme(jixOneDef, darkTheme) }
    } else {
        // Fallback (a stored legacy seed from a 4.x install): materialKolor
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

    // Ambient mesh — the web app's signature background (globals.css body):
    // three soft theme-colored blobs over a vertical base gradient. Persian
    // themes carry the exact web colors; every other theme derives them from
    // its color scheme. Skipped entirely for pure black (user asked for black).
    val mesh = if (pureBlack) {
        null
    } else {
        persianTheme?.let { theme ->
            val blobs = if (darkTheme) theme.meshDark else theme.meshLight
            val base = if (darkTheme) {
                theme.darkBackground ?: baseColorScheme.background
            } else {
                theme.lightBackground ?: baseColorScheme.background
            }
            val base2 = if (darkTheme) {
                theme.darkBg2 ?: base
            } else {
                theme.lightBg2 ?: base
            }
            blobs?.let {
                ThemeMesh(
                    blobTopRight = it.first,
                    blobBottomLeft = it.second,
                    blobBottomCenter = it.third,
                    baseTop = base,
                    baseBottom = base2,
                )
            }
        } ?: run {
            val c = baseColorScheme
            // Web direction: the base gradient brightens toward the bottom
            // (dark themes lift toward surfaceContainer, light toward white).
            val base2Dark = lerp(c.surface, c.surfaceContainer, 0.65f)
            val base2Light = lerp(c.surface, Color.White, 0.55f)
            if (darkTheme) {
                ThemeMesh(
                    blobTopRight = c.primary.copy(alpha = 0.14f),
                    blobBottomLeft = c.tertiary.copy(alpha = 0.11f),
                    blobBottomCenter = c.secondary.copy(alpha = 0.08f),
                    baseTop = c.surface,
                    baseBottom = base2Dark,
                )
            } else {
                ThemeMesh(
                    blobTopRight = c.primary.copy(alpha = 0.09f),
                    blobBottomLeft = c.tertiary.copy(alpha = 0.07f),
                    blobBottomCenter = c.secondary.copy(alpha = 0.05f),
                    baseTop = c.surface,
                    baseBottom = base2Light,
                )
            }
        }
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    CompositionLocalProvider(
        LocalThemeArtwork provides artwork,
        LocalThemeMesh provides mesh,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = JixOneShapes,
            typography = JixOneTypography,
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
