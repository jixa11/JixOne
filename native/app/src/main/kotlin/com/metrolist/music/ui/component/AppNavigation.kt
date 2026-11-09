/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        // JixOne: glass rail — slightly translucent so the ambient mesh
        // breathes through, like the web sidebar over the body gradient.
        containerColor = if (pureBlack) containerColor else containerColor.copy(alpha = 0.94f)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem || isHomeHoldItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (!isSearchItem && !isHomeHoldItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * JixOne "Aurora Island" bottom navigation (5.1 redesign).
 *
 * A detached floating pill that hovers above the gesture area over the app's
 * ambient mesh background — the signature pattern of the newest music apps
 * (Material 3 Expressive / iOS-style floating tab docks). The active item
 * morphs into a gradient (primary -> tertiary) pill that grows to reveal its
 * label; inactive items are quiet monochrome icons. Press morphs are driven
 * by animateContentSize springs, taps keep the upstream long-press behaviors
 * (search = song recognition, home = account switcher).
 */
@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    onHomeLongHold: (() -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    val barHeight = if (slimNav) 58.dp else 66.dp
    val pillHeight = if (slimNav) 32.dp else 38.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
                .height(barHeight),
            shape = RoundedCornerShape(barHeight / 2),
            color = if (pureBlack) Color(0xFF111116).copy(alpha = 0.96f)
            else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.93f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
            ),
            shadowElevation = 18.dp,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationItems.forEach { screen ->
                    val isSelected = remember(currentRoute, screen.route) {
                        isRouteSelected(currentRoute, screen.route, navigationItems)
                    }
                    val currentIsSelected by rememberUpdatedState(isSelected)
                    val iconRes = remember(isSelected, screen) {
                        if (isSelected) screen.iconIdActive else screen.iconIdInactive
                    }

                    val targetContentColor =
                        if (isSelected) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    val contentColor by animateColorAsState(
                        targetValue = targetContentColor,
                        animationSpec = tween(durationMillis = 200),
                        label = "navItemColor",
                    )

                    val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                    val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
                    val interactionSource = remember { MutableInteractionSource() }

                    // Long press detection using InteractionSource (kept from
                    // upstream: search long-press = song recognition, home
                    // long-hold = account switcher)
                    if (isSearchItem || isHomeHoldItem) {
                        LaunchedEffect(interactionSource) {
                            var isLongClick = false
                            interactionSource.interactions.collectLatest { interaction ->
                                when (interaction) {
                                    is PressInteraction.Press -> {
                                        isLongClick = false
                                        delay(if (isHomeHoldItem) 15_000L else viewConfiguration.longPressTimeoutMillis)
                                        isLongClick = true
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isHomeHoldItem) onHomeLongHold.invoke() else onSearchLongClick?.invoke()
                                    }
                                    is PressInteraction.Release -> {
                                        if (!isLongClick) {
                                            onItemClick(screen, currentIsSelected)
                                        }
                                    }
                                    is PressInteraction.Cancel -> {
                                        isLongClick = false
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        // The morphing indicator pill: gradient-filled and
                        // revealing the label when active; a bare icon when not.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                )
                                .height(pillHeight)
                                .clip(RoundedCornerShape(pillHeight / 2))
                                .background(
                                    if (isSelected) {
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary,
                                            ),
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color.Transparent, Color.Transparent),
                                        )
                                    },
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                ) {
                                    // Special items (search long-press / home
                                    // long-hold) are driven by the InteractionSource
                                    // above; their normal click fires on Release.
                                    if (!isSearchItem && !isHomeHoldItem) {
                                        onItemClick(screen, currentIsSelected)
                                    }
                                }
                                .padding(horizontal = 14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = stringResource(screen.titleId),
                                    tint = contentColor,
                                    modifier = Modifier.size(if (slimNav) 20.dp else 22.dp),
                                )
                                if (isSelected) {
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = stringResource(screen.titleId),
                                        fontSize = 12.5.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
