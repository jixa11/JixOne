/**
 * JixOne Persian artwork themes.
 *
 * Ported 1:1 from the JixOne web app (src/lib/themes.ts): the four Persian
 * themes carry real artwork (carpet / Isfahan tilework / Derafsh Kaviani /
 * Cyrus relief) behind the whole app, veiled so text keeps full contrast —
 * exactly like the original web app looked.
 */

package com.metrolist.music.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.metrolist.music.R

/** The JixOne app-default seed: Persian Carpet gold (matches the web app). */
val PersianCarpetSeed = Color(0xFFD9A441)
val PersianTileSeed = Color(0xFF33C5D6)
val DerafshKavianiSeed = Color(0xFF9A5BE0)
val CyrusSeed = Color(0xFF2A5D9F)

data class PersianArtworkTheme(
    /** seed color the theme picker stores — used as the identity match */
    val seed: Color,
    /** web-app background color used for the dark veil (null = generated) */
    val darkBackground: Color?,
    /** web-app background color for light mode (null = generated) */
    val lightBackground: Color?,
    /** full-bleed artwork drawable behind the app */
    val artworkRes: Int,
    /** true = seamless motif, repeated (tiled) instead of cropped */
    val tiled: Boolean,
)

/**
 * Everything the app root needs to paint an active artwork background.
 * Provided by [MetrolistTheme] via [LocalThemeArtwork].
 */
class ThemeArtwork(
    val artworkRes: Int,
    val tiled: Boolean,
    /** veil drawn over the artwork; keeps text at full contrast */
    val veilColor: Color,
)

val LocalThemeArtwork = staticCompositionLocalOf<ThemeArtwork?> { null }

val PersianArtworkThemes = listOf(
    PersianArtworkTheme(
        seed = PersianCarpetSeed,
        darkBackground = Color(0xFF081828), // deep carpet navy (web app)
        lightBackground = null,
        artworkRes = R.drawable.jixone_bg_carpet,
        tiled = false,
    ),
    PersianArtworkTheme(
        seed = PersianTileSeed,
        darkBackground = Color(0xFF081226), // tilework midnight (web app)
        lightBackground = null,
        artworkRes = R.drawable.jixone_bg_tile,
        tiled = true,
    ),
    PersianArtworkTheme(
        seed = DerafshKavianiSeed,
        darkBackground = Color(0xFF1B0A2A), // kaviani plum (web app)
        lightBackground = null,
        artworkRes = R.drawable.jixone_bg_derafsh,
        tiled = false,
    ),
    PersianArtworkTheme(
        seed = CyrusSeed,
        darkBackground = Color(0xFF16233F), // darkened achaemenid blue
        lightBackground = Color(0xFFF5EEDE), // cyrus parchment (web app)
        artworkRes = R.drawable.jixone_bg_cyrus,
        tiled = false,
    ),
)

/** Returns the Persian artwork theme whose seed matches, or null. */
fun persianThemeFor(seed: Color): PersianArtworkTheme? =
    PersianArtworkThemes.firstOrNull { it.seed == seed }
