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
    /** how much veil covers the art in dark mode — 1 - (web art opacity) */
    val veilAlphaDark: Float = 0.70f,
    /** how much veil covers the art in light mode */
    val veilAlphaLight: Float = 0.80f,
    /** ambient mesh blobs under the artwork (dark mode) — web globals.css --mesh-1/2/3 */
    val meshDark: Triple<Color, Color, Color>? = null,
    /** ambient mesh blobs (light mode) */
    val meshLight: Triple<Color, Color, Color>? = null,
    /** vertical-gradient partner of the dark bg — web --app-bg-2 (null = derive) */
    val darkBg2: Color? = null,
    /** vertical-gradient partner of the light bg — web --app-bg-2 (null = derive) */
    val lightBg2: Color? = null,
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
        // web: body::after opacity .30 → veil .70 (globals.css persian-carpet)
        veilAlphaDark = 0.70f,
        // web mesh: gold 15% / copper 12% / lapis 12% (globals.css persian-carpet)
        meshDark = Triple(
            Color(0x26D9A441),
            Color(0x1FC8763A),
            Color(0x1F2860A0),
        ),
        // web --app-bg-2 (globals.css persian-carpet)
        darkBg2 = Color(0xFF0C2136),
    ),
    PersianArtworkTheme(
        seed = PersianTileSeed,
        darkBackground = Color(0xFF081226), // tilework midnight (web app)
        lightBackground = null,
        artworkRes = R.drawable.jixone_bg_tile,
        tiled = true,
        // web: opacity .34 → veil .66 (globals.css persian-tile)
        veilAlphaDark = 0.66f,
        // web mesh: turquoise 16% / lapis 16% / saffron 8% (globals.css persian-tile)
        meshDark = Triple(
            Color(0x2933C5D6),
            Color(0x294F7FE6),
            Color(0x14D8AA50),
        ),
        // web --app-bg-2 (globals.css persian-tile)
        darkBg2 = Color(0xFF0C1A33),
    ),
    PersianArtworkTheme(
        seed = DerafshKavianiSeed,
        darkBackground = Color(0xFF1B0A2A), // kaviani plum (web app)
        lightBackground = null,
        artworkRes = R.drawable.jixone_bg_derafsh,
        tiled = false,
        // web: opacity .32 → veil .68 (globals.css derafsh)
        veilAlphaDark = 0.68f,
        // web mesh: gold 15% / amethyst 17% / madder 9% (globals.css derafsh)
        meshDark = Triple(
            Color(0x26E0B24A),
            Color(0x2B9A5BE0),
            Color(0x17C83250),
        ),
        // web --app-bg-2 (globals.css derafsh)
        darkBg2 = Color(0xFF25103A),
    ),
    PersianArtworkTheme(
        seed = CyrusSeed,
        darkBackground = Color(0xFF16233F), // darkened achaemenid blue
        lightBackground = Color(0xFFF5EEDE), // cyrus parchment (web app)
        artworkRes = R.drawable.jixone_bg_cyrus,
        tiled = false,
        // web: opacity .20 on the light parchment → veil .80 (globals.css cyrus)
        veilAlphaLight = 0.80f,
        // web mesh (light): gold 13% / lapis 11% / terracotta 9% (globals.css cyrus)
        meshLight = Triple(
            Color(0x21B08A2E),
            Color(0x1C2A5D9F),
            Color(0x17C46240),
        ),
        // web --app-bg-2 (globals.css cyrus)
        lightBg2 = Color(0xFFFBF6EA),
    ),
)

/** Returns the Persian artwork theme whose seed matches, or null. */
fun persianThemeFor(seed: Color): PersianArtworkTheme? =
    PersianArtworkThemes.firstOrNull { it.seed == seed }

/**
 * The ambient mesh the app root paints behind everything: three soft color
 * blobs at the screen corners over a vertical base gradient — the signature
 * look of the JixOne web app (globals.css `body` background), ported 1:1.
 * Provided by [MetrolistTheme] via [LocalThemeMesh].
 */
class ThemeMesh(
    val blobTopRight: Color,
    val blobBottomLeft: Color,
    val blobBottomCenter: Color,
    /** top color of the vertical base gradient (web: --app-bg) */
    val baseTop: Color,
    /** bottom color of the vertical base gradient (web: --app-bg-2) */
    val baseBottom: Color,
)

val LocalThemeMesh = staticCompositionLocalOf<ThemeMesh?> { null }
