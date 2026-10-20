/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.metrolist.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    // JixOne web parity: the mobile nav is Search -> Explore -> Library
    // (no Home tab, no settings tab — settings lives in the top-bar gear,
    // exactly like the web app's MobileNav). The "Home" object below KEEPS
    // its "home" route so deep links and the nav graph stay untouched, but
    // its visible identity is the web app's Explore tab (Compass icon).
    object Home : Screens(
        titleId = R.string.explore,
        iconIdInactive = R.drawable.jix_compass,
        iconIdActive = R.drawable.jix_compass_filled,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.jix_search,
        iconIdActive = R.drawable.jix_search_bold,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.group_outlined,
        iconIdActive = R.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.jix_library,
        iconIdActive = R.drawable.jix_library_filled,
        route = "library"
    )

    companion object {
        // Web MobileNav order: Search first, then Explore, then Library.
        // ListenTogether only appears here when moved out of the top bar.
        val MainScreens = listOf(Search, Home, ListenTogether, Library)
    }
}
