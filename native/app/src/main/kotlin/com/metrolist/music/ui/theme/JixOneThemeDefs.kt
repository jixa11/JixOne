/**
 * JixOne 5.0 — the app's own 12 themes, ported 1:1 from the web app
 * (src/lib/themes.ts) with FULL hand-tuned color schemes instead of
 * materialkolor tonal-spot seeding. Every theme gets its real web palette:
 * background tones, accent, accent-2 and mesh highlight — so the app finally
 * reads "JixOne", not "Metrolist with a recolor".
 *
 * Seeds double as stable identities persisted in SelectedThemeColorKey.
 */

package com.metrolist.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.metrolist.music.R

data class JixOneThemeDef(
    /** persisted identity (stored in SelectedThemeColorKey) */
    val seed: Color,
    /** localized display name */
    val nameRes: Int,
    /** web --app-bg (dark look) */
    val darkBg: Color,
    /** web --app-bg (light look; used when theme defaults light) */
    val lightBg: Color,
    /** web --accent */
    val accent: Color,
    /** web --accent-2 */
    val accent2: Color,
    /** web --mesh highlight / tertiary */
    val highlight: Color,
    /** theme's home look is light (Cyrus, YouTube Light) */
    val defaultsLight: Boolean = false,
    /** Persian artwork drawable (the four Persian themes) */
    val artworkRes: Int? = null,
    val tiled: Boolean = false,
)

/** The 12 JixOne web themes — the ONLY themes the picker offers. */
val JixOneThemes: List<JixOneThemeDef> = listOf(
    JixOneThemeDef( // persian-carpet
        seed = Color(0xFFD9A441),
        nameRes = R.string.palette_jixone_carpet,
        darkBg = Color(0xFF081828),
        lightBg = Color(0xFFF3E9D5),
        accent = Color(0xFFD9A441),
        accent2 = Color(0xFFC8763A),
        highlight = Color(0xFFE8D9B0),
        artworkRes = R.drawable.jixone_bg_carpet,
    ),
    JixOneThemeDef( // persian-tile
        seed = Color(0xFF33C5D6),
        nameRes = R.string.palette_jixone_tile,
        darkBg = Color(0xFF081226),
        lightBg = Color(0xFFE3F0F4),
        accent = Color(0xFF33C5D6),
        accent2 = Color(0xFF4F7FE6),
        highlight = Color(0xFFD8AA50),
        artworkRes = R.drawable.jixone_bg_tile,
        tiled = true,
    ),
    JixOneThemeDef( // derafsh
        seed = Color(0xFF9A5BE0),
        nameRes = R.string.palette_jixone_derafsh,
        darkBg = Color(0xFF1B0A2A),
        lightBg = Color(0xFFEFE6F5),
        accent = Color(0xFFE0B24A),
        accent2 = Color(0xFF9A5BE0),
        highlight = Color(0xFFC83250),
        artworkRes = R.drawable.jixone_bg_derafsh,
    ),
    JixOneThemeDef( // cyrus — light parchment look
        seed = Color(0xFF2A5D9F),
        nameRes = R.string.palette_jixone_cyrus,
        darkBg = Color(0xFF16233F),
        lightBg = Color(0xFFF5EEDE),
        accent = Color(0xFFB08A2E),
        accent2 = Color(0xFF2A5D9F),
        highlight = Color(0xFFC46240),
        defaultsLight = true,
        artworkRes = R.drawable.jixone_bg_cyrus,
    ),
    JixOneThemeDef( // cyber-red / Neon Crimson
        seed = Color(0xFFFF2D55),
        nameRes = R.string.palette_neon_crimson,
        darkBg = Color(0xFF08070C),
        lightBg = Color(0xFFF6EDEF),
        accent = Color(0xFFFF2D55),
        accent2 = Color(0xFF8B5CF6),
        highlight = Color(0xFF22D3EE),
    ),
    JixOneThemeDef( // cyber-blue / Neo Tokyo
        seed = Color(0xFF22D3EE),
        nameRes = R.string.palette_neo_tokyo,
        darkBg = Color(0xFF04060E),
        lightBg = Color(0xFFE6F1F5),
        accent = Color(0xFF22D3EE),
        accent2 = Color(0xFF6366F1),
        highlight = Color(0xFFD946EF),
    ),
    JixOneThemeDef( // synthwave
        seed = Color(0xFFFF2FA0),
        nameRes = R.string.palette_synthwave,
        darkBg = Color(0xFF0C0416),
        lightBg = Color(0xFFF7E9F3),
        accent = Color(0xFFFF2FA0),
        accent2 = Color(0xFFA855F7),
        highlight = Color(0xFFFB923C),
    ),
    JixOneThemeDef( // classic spotify
        seed = Color(0xFF1DB954),
        nameRes = R.string.palette_classic_spotify,
        darkBg = Color(0xFF0E0E0E),
        lightBg = Color(0xFFEDF4EE),
        accent = Color(0xFF1DB954),
        accent2 = Color(0xFF14B8A6),
        highlight = Color(0xFF3B82F6),
    ),
    JixOneThemeDef( // pure oled
        seed = Color(0xFFE4E4E7),
        nameRes = R.string.palette_pure_oled,
        darkBg = Color(0xFF000000),
        lightBg = Color(0xFFF4F4F5),
        accent = Color(0xFFE4E4E7),
        accent2 = Color(0xFFA1A1AA),
        highlight = Color(0xFF71717A),
    ),
    JixOneThemeDef( // dubai sunset
        seed = Color(0xFFFF6B35),
        nameRes = R.string.palette_dubai_sunset,
        darkBg = Color(0xFF0E0910),
        lightBg = Color(0xFFFAEFE8),
        accent = Color(0xFFFF6B35),
        accent2 = Color(0xFFF59E0B),
        highlight = Color(0xFFEC4899),
    ),
    JixOneThemeDef( // yt-light — light look
        seed = Color(0xFFFF5E3A),
        nameRes = R.string.palette_youtube_light,
        darkBg = Color(0xFF121216),
        lightBg = Color(0xFFF7F7F9),
        accent = Color(0xFFFF0033),
        accent2 = Color(0xFFFF5E3A),
        highlight = Color(0xFF8B5CF6),
        defaultsLight = true,
    ),
    JixOneThemeDef( // yt-original
        seed = Color(0xFFFF0033),
        nameRes = R.string.palette_youtube,
        darkBg = Color(0xFF0D0D0D),
        lightBg = Color(0xFFF7EDED),
        accent = Color(0xFFFF0033),
        accent2 = Color(0xFFFF6B6B),
        highlight = Color(0xFFB45309),
    ),
)

/** Returns the JixOne web theme whose seed matches, or null. */
fun jixOneThemeFor(seed: Color): JixOneThemeDef? =
    JixOneThemes.firstOrNull { it.seed == seed }

/** Readable text color on top of the given accent. */
private fun onColorOf(color: Color): Color =
    if (color.luminance() > 0.45f) Color(0xFF101014) else Color(0xFFFAFAFC)

/**
 * Builds a full Material3 ColorScheme from a web theme definition.
 * Surfaces are lifted from the theme's own background tone (not neutral
 * gray like materialkolor), and primary/secondary/tertiary carry the
 * theme's real accent / accent-2 / highlight — the web app's CSS variables,
 * translated to Compose.
 */
fun buildJixOneColorScheme(def: JixOneThemeDef, dark: Boolean): ColorScheme {
    val base = if (dark) def.darkBg else def.lightBg
    val lift = if (dark) Color.White else Color.Black
    // how much surfaces lift off the background, per elevation tier
    val sLow = if (dark) 0.035f else 0.025f
    val sCont = if (dark) 0.065f else 0.045f
    val sHigh = if (dark) 0.10f else 0.075f
    val sHighest = if (dark) 0.14f else 0.11f
    val sVariant = if (dark) 0.085f else 0.06f

    // container tint ratios + container label colors
    val contTint = if (dark) 0.30f else 0.16f
    val onCont = if (dark) {
        { c: Color -> lerp(Color.White, c, 0.18f) }
    } else {
        { c: Color -> lerp(Color.Black, c, 0.35f) }
    }

    return androidx.compose.material3.ColorScheme(
        primary = def.accent,
        onPrimary = onColorOf(def.accent),
        primaryContainer = lerp(base, def.accent, contTint),
        onPrimaryContainer = onCont(def.accent),
        inversePrimary = def.accent,
        secondary = def.accent2,
        onSecondary = onColorOf(def.accent2),
        secondaryContainer = lerp(base, def.accent2, contTint),
        onSecondaryContainer = onCont(def.accent2),
        tertiary = def.highlight,
        onTertiary = onColorOf(def.highlight),
        tertiaryContainer = lerp(base, def.highlight, contTint * 0.9f),
        onTertiaryContainer = onCont(def.highlight),
        background = base,
        onBackground = lerp(base, lift, if (dark) 0.92f else 0.87f),
        surface = base,
        onSurface = lerp(base, lift, if (dark) 0.92f else 0.87f),
        surfaceVariant = lerp(base, lift, sVariant),
        onSurfaceVariant = lerp(base, lift, if (dark) 0.62f else 0.58f),
        surfaceTint = def.accent,
        inverseSurface = lerp(base, lift, 0.90f),
        inverseOnSurface = base,
        error = if (dark) Color(0xFFFF5A6E) else Color(0xFFB3261E),
        onError = if (dark) Color(0xFF2A060B) else Color(0xFFFDECEE),
        errorContainer = lerp(base, Color(0xFFFF5A6E), if (dark) 0.22f else 0.14f),
        onErrorContainer = onCont(Color(0xFFFF5A6E)),
        outline = lerp(base, lift, if (dark) 0.32f else 0.35f),
        outlineVariant = lerp(base, lift, if (dark) 0.16f else 0.16f),
        scrim = Color.Black,
        surfaceBright = lerp(base, lift, if (dark) 0.22f else 1f),
        surfaceDim = lerp(base, Color.Black, if (dark) 0.30f else 0.06f),
        surfaceContainer = lerp(base, lift, sCont),
        surfaceContainerHigh = lerp(base, lift, sHigh),
        surfaceContainerHighest = lerp(base, lift, sHighest),
        surfaceContainerLow = lerp(base, lift, sLow),
        surfaceContainerLowest = if (dark) Color.Black else lerp(base, Color.White, 0.6f),
    )
}
