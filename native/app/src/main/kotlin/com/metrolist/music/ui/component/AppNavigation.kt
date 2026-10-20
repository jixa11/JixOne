/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 * JixOne bottom navigation — the modern compact bar (Spotify / YT Music
 * flavor): small precise icons, the selected item sits in a soft tonal
 * chip, filled icon variants when active, tiny labels. Nothing like the
 * old oversized pill-indicator bar.
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        // glass — the bar sits at 92% surfaceContainer so a breath of the
        // ambient mesh / Persian artwork glows through.
        color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 0.dp,
    ) {
        Column {
            // hairline top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (slimNav) 2.dp else 5.dp),
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
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    val contentColor by animateColorAsState(
                        targetValue = targetContentColor,
                        animationSpec = tween(durationMillis = 150),
                        label = "navItemColor",
                    )
                    val chipAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "navChipAlpha",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = tween(durationMillis = 150),
                        label = "navIconScale",
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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                if (!isSearchItem && !isHomeHoldItem) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            .padding(vertical = if (slimNav) 1.dp else 3.dp),
                    ) {
                        // modern active treatment: the icon sits inside a soft
                        // tonal chip (like YTM's selected pill behind the icon)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(30.dp)
                                .fillMaxWidth(0.62f)
                                .clip(RoundedCornerShape(15.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.14f * chipAlpha
                                    )
                                ),
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = stringResource(screen.titleId),
                                tint = contentColor,
                                modifier = Modifier
                                    .size(if (slimNav) 20.dp else 21.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                        }
                        if (!slimNav) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(screen.titleId),
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = contentColor,
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
