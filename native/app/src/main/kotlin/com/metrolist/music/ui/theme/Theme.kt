/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

// JixOne 6: every theme is a curated seed color — the pre-6 "follow system
// wallpaper" behavior is gone, so the default color is simply Aurora, the
// app's identity palette (same value as DefaultPaletteSeed).
val DefaultThemeColor = Color(0xFF7C5CFF)

/** Pre-6.0 sentinel stored when the removed "Dynamic / system color" palette
 *  was selected — kept only so the one-time migration can move those
 *  installs to Aurora. */
val LegacyDynamicSentinelColor = Color(0xFF5F5BF9)

/**
 * JixOne modern shape language: generous, soft corners everywhere —
 * the visual signature that separates JixOne from stock Material.
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
fun JixOneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    showArtwork: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Persian artwork theme (JixOne): web-app backgrounds + full-bleed art
    val persianTheme = if (showArtwork) persianThemeFor(themeColor) else null

    // JixOne 6: the palette list is a fixed, curated set of seeds — every
    // scheme is generated from the seed with materialKolor (Expressive for
    // richer secondary/tertiary chroma so the primary -> tertiary gradients
    // actually glow). No more system-wallpaper dynamic color.
    val baseColorScheme = rememberDynamicColorScheme(
        seedColor = themeColor,
        isDark = darkTheme,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.Expressive
    )

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
        // JixOne 6 button-clarity pass — run BEFORE the artwork transparency
        // so every palette (Derafsh violet, OLED neutral, …) keeps readable
        // controls in both light and dark mode.
        scheme = scheme.repairButtonContrast()
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
                    blobTopRight = c.primary.copy(alpha = 0.22f),
                    blobBottomLeft = c.tertiary.copy(alpha = 0.17f),
                    blobBottomCenter = c.secondary.copy(alpha = 0.12f),
                    baseTop = c.surface,
                    baseBottom = base2Dark,
                )
            } else {
                ThemeMesh(
                    blobTopRight = c.primary.copy(alpha = 0.13f),
                    blobBottomLeft = c.tertiary.copy(alpha = 0.10f),
                    blobBottomCenter = c.secondary.copy(alpha = 0.07f),
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

// ──────────────────────────────────────────────────────────────────────────
// JixOne 6: the button-clarity pass.
//
// Seed-generated schemes occasionally produce container/content pairs with
// too little contrast to read a control (the Derafsh Kaviani violet and the
// OLED neutral were the reported offenders). This pass walks the pairs that
// drive buttons, chips and toggles and, whenever a pair falls below a 3.5:1
// ratio, replaces the content color with the black/white variant that reads
// best on that container. Palettes that already pass are left untouched.
// ─────────────────────────────────────────────────────────────────────────

private fun Color.relativeLuminance(): Float {
    fun channel(c: Float) =
        if (c <= 0.03928f) c / 12.92f
        else Math.pow(((c + 0.055) / 1.055).toDouble(), 2.4).toFloat()
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}

private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.relativeLuminance() + 0.05f
    val lb = b.relativeLuminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/** The black/white variant with the stronger contrast on [container]. */
private fun bestInkOn(container: Color): Color =
    if (contrastRatio(Color.Black, container) >= contrastRatio(Color.White, container))
        Color.Black
    else
        Color.White

private const val ButtonContrastFloor = 3.5f

private fun ColorScheme.repairPair(
    container: Color,
    content: Color,
): Color =
    if (contrastRatio(content, container) < ButtonContrastFloor) bestInkOn(container) else content

fun ColorScheme.repairButtonContrast(): ColorScheme {
    var s = this

    s = s.copy(onPrimary = s.repairPair(s.primary, s.onPrimary))
    s = s.copy(onPrimaryContainer = s.repairPair(s.primaryContainer, s.onPrimaryContainer))
    s = s.copy(onSecondary = s.repairPair(s.secondary, s.onSecondary))
    s = s.copy(onSecondaryContainer = s.repairPair(s.secondaryContainer, s.onSecondaryContainer))
    s = s.copy(onTertiary = s.repairPair(s.tertiary, s.onTertiary))
    s = s.copy(onTertiaryContainer = s.repairPair(s.tertiaryContainer, s.onTertiaryContainer))
    s = s.copy(onError = s.repairPair(s.error, s.onError))
    s = s.copy(onBackground = s.repairPair(s.background, s.onBackground))
    s = s.copy(onSurface = s.repairPair(s.surface, s.onSurface))
    s = s.copy(onSurfaceVariant = s.repairPair(s.surfaceVariant, s.onSurfaceVariant))

    // Player/list secondary buttons draw onSurface content over these
    // elevated containers — if the (already-repaired) ink still fails on the
    // button container, flip it to the variant that reads there; surface
    // tones stay close enough that the flip keeps both readable.
    var surfaceInk = s.onSurface
    if (contrastRatio(surfaceInk, s.surfaceContainerHighest) < ButtonContrastFloor) {
        surfaceInk = bestInkOn(s.surfaceContainerHighest)
    }
    s = s.copy(onSurface = surfaceInk)

    var variantInk = s.onSurfaceVariant
    if (contrastRatio(variantInk, s.surfaceContainerHigh) < ButtonContrastFloor) {
        variantInk = bestInkOn(s.surfaceContainerHigh)
    }
    s = s.copy(onSurfaceVariant = variantInk)

    return s
}

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
