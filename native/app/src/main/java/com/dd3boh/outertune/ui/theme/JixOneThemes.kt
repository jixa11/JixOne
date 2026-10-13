/*
 * JixOne signature themes, ported from the JixOne web app (src/lib/themes.ts).
 * Each theme maps its brand colors onto Material 3 tonal palettes, so every
 * component of the app (surfaces, containers, player, menus, notifications)
 * follows the theme automatically through DynamicScheme.
 */
package com.dd3boh.outertune.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.google.material.color.dynamiccolor.DynamicScheme
import com.google.material.color.dynamiccolor.Variant
import com.google.material.color.hct.Hct
import com.google.material.color.palettes.TonalPalette
import java.util.Optional

enum class JixOneTheme {
    DEFAULT,
    PERSIAN_CARPET,
    ISFAHAN_TILE,
    KAVIANI_BANNER,
    CYRUS;

    /** Brand swatch for the settings picker, same order as the web app. */
    fun swatch(): List<Color> = when (this) {
        DEFAULT -> listOf(Color(0xFFED5564))
        PERSIAN_CARPET -> listOf(Color(0xFFD9A441), Color(0xFFC8763A), Color(0xFFE8D9B0), Color(0xFF081828))
        ISFAHAN_TILE -> listOf(Color(0xFF33C5D6), Color(0xFF4F7FE6), Color(0xFFD8AA50), Color(0xFF081226))
        KAVIANI_BANNER -> listOf(Color(0xFFE0B24A), Color(0xFF9A5BE0), Color(0xFFC83250), Color(0xFF1B0A2A))
        CYRUS -> listOf(Color(0xFFB08A2E), Color(0xFF2A5D9F), Color(0xFFC46240), Color(0xFFF5EEDE))
    }
}

/**
 * A DynamicScheme built directly from the JixOne brand colors: primary,
 * secondary and tertiary accents keep their exact hues, while the neutral
 * ramp inherits the tint of the theme's background color so surfaces feel
 * like the web app's "veiled artwork" backgrounds.
 */
private class JixOneScheme(
    isDark: Boolean,
    primary: Int,
    secondary: Int,
    tertiary: Int,
    neutral: Int,
    neutralVariant: Int,
) : DynamicScheme(
    /* sourceColorHct     */ Hct.fromInt(primary),
    /* variant            */ Variant.TONAL_SPOT,
    /* isDark             */ isDark,
    /* contrastLevel      */ 0.0,
    /* primaryPalette     */ TonalPalette.fromInt(primary),
    /* secondaryPalette   */ TonalPalette.fromInt(secondary),
    /* tertiaryPalette    */ TonalPalette.fromInt(tertiary),
    /* neutralPalette     */ TonalPalette.fromInt(neutral),
    /* neutralVariantPalette */ TonalPalette.fromInt(neutralVariant),
    /* errorPalette       */ Optional.of(TonalPalette.fromInt(0xFFB3261E.toInt())),
)

fun jixOneColorScheme(theme: JixOneTheme, isDark: Boolean): ColorScheme =
    when (theme) {
        // Persian Carpet - فرش ایرانی: antique gold, copper madder, cream, midnight indigo
        JixOneTheme.PERSIAN_CARPET -> JixOneScheme(
            isDark,
            primary = 0xFFD9A441.toInt(),
            secondary = 0xFFC8763A.toInt(),
            tertiary = 0xFFE8D9B0.toInt(),
            neutral = 0xFF081828.toInt(),
            neutralVariant = 0xFF22344A.toInt(),
        )
        // Isfahan Tile - کاشی اصفهان: turquoise, lapis blue, gold, deep navy
        JixOneTheme.ISFAHAN_TILE -> JixOneScheme(
            isDark,
            primary = 0xFF33C5D6.toInt(),
            secondary = 0xFF4F7FE6.toInt(),
            tertiary = 0xFFD8AA50.toInt(),
            neutral = 0xFF081226.toInt(),
            neutralVariant = 0xFF23344D.toInt(),
        )
        // Kaviani Banner - درفش کاویانی: royal gold, purple, crimson, night violet
        JixOneTheme.KAVIANI_BANNER -> JixOneScheme(
            isDark,
            primary = 0xFFE0B24A.toInt(),
            secondary = 0xFF9A5BE0.toInt(),
            tertiary = 0xFFC83250.toInt(),
            neutral = 0xFF1B0A2A.toInt(),
            neutralVariant = 0xFF3A2B4A.toInt(),
        )
        // Cyrus the Great - کوروش هخامنشی: Achaemenid gold, lapis, terracotta, parchment
        JixOneTheme.CYRUS -> JixOneScheme(
            isDark,
            primary = 0xFFB08A2E.toInt(),
            secondary = 0xFF2A5D9F.toInt(),
            tertiary = 0xFFC46240.toInt(),
            neutral = 0xFFF5EEDE.toInt(),
            neutralVariant = 0xFFD8CFBB.toInt(),
        )
        JixOneTheme.DEFAULT -> throw IllegalArgumentException("DEFAULT theme has no color scheme")
    }.toColorScheme()
