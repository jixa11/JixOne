/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
 * JixOne "Vivid Dock" bottom navigation.
 *
 * A floating glass dock with a gradient rim. Every destination keeps its
 * label (no guessing what an icon means); the active one lifts into a
 * glowing gradient badge and its label takes the accent color. Taps keep the
 * long-press behaviors (search = song recognition, home = account switcher).
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
    val badgeSize = if (slimNav) 40.dp else 36.dp
    // vertical breathing room around the dock; slim mode stays within its
    // 64dp reserved slot (58 + 2*3), regular fits the 80dp slot (66 + 2*6)
    val barVerticalPadding = if (slimNav) 3.dp else 6.dp
    val jixAccent = com.metrolist.music.ui.jix.LocalJixAccent.current
    val accent = jixAccent.start
    val accentBrush = jixAccent.brush
    val accentInk = jixAccent.ink
    val labelAccent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = barVerticalPadding)
                .fillMaxWidth()
                .height(barHeight),
            shape = RoundedCornerShape(26.dp),
            color = if (pureBlack) Color(0xFF111116).copy(alpha = 0.96f)
            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                    ),
                ),
            ),
            shadowElevation = 20.dp,
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

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) accentInk
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 200),
                        label = "navIconColor",
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) labelAccent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 200),
                        label = "navLabelColor",
                    )
                    val badgeScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.86f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "navBadgeScale",
                    )

                    val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                    val isHomeHoldItem = screen == Screens.Home && onHomeLongHold != null
                    val interactionSource = remember { MutableInteractionSource() }

                    // Long press detection using InteractionSource: search
                    // long-press = song recognition, home long-hold = account
                    // switcher.
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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
                            },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(badgeScale)
                                .size(width = badgeSize + 18.dp, height = badgeSize)
                                .then(
                                    if (isSelected) {
                                        Modifier.shadow(
                                            elevation = 10.dp,
                                            shape = RoundedCornerShape(badgeSize / 2),
                                            ambientColor = accent,
                                            spotColor = accent,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .clip(RoundedCornerShape(badgeSize / 2))
                                .background(
                                    if (isSelected) {
                                        accentBrush
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color.Transparent, Color.Transparent),
                                        )
                                    },
                                ),
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = stringResource(screen.titleId),
                                tint = iconColor,
                                modifier = Modifier.size(if (slimNav) 26.dp else 24.dp),
                            )
                        }
                        if (!slimNav) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = stringResource(screen.titleId),
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = labelColor,
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
