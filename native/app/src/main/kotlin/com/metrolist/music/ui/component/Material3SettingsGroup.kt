/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * JixOne modern settings group — the Spotify-style settings list: ONE soft
 * rounded card per group, items separated by hairlines, each item led by a
 * tonal icon chip. Themed entirely through MaterialTheme so every JixOne
 * theme (incl. Persian artwork) stays harmonious.
 */
@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    useLowContrast: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Section title
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp),
            )
        }

        // One card per group, hairline-separated rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (!useLowContrast) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)
                    }
                )
                .animateContentSize(),
        ) {
            items.forEachIndexed { index, item ->
                Material3SettingsItemRow(item = item)
                if (index != items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 72.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    )
                }
            }
        }
    }
}

/**
 * Individual settings item row — tonal icon chip + title + description.
 */
@Composable
private fun Material3SettingsItemRow(
    item: Material3SettingsItem,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.enabled && item.onClick != null,
                onClick = { item.onClick?.invoke() }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom leading content or tonal icon chip
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(modifier = Modifier.width(16.dp))
        } else if (item.icon != null) {
            val tint = if (!item.enabled)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else
                item.iconTint ?: MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                if (item.showBadge) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        }
                    ) {
                        Icon(
                            painter = item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))
        }

        // Title and description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    color = if (!item.enabled)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
            ) {
                item.title()
            }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(
                        color = if (!item.enabled)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                ) {
                    desc()
                }
            }
        }

        // Trailing content
        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Data class for a settings item. `iconTint` colors the tonal chip — pass a
 * theme role (primary / tertiary / secondary) for the multi-hue Spotify look.
 */
data class Material3SettingsItem(
    val icon: Painter? = null,
    val iconTint: Color? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
)
