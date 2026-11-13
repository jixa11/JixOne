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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
 * JixOne 6 curated section accents — mid-tone hues chosen to read on both
 * light and dark surfaces. Groups get one of these by stable title hash so
 * every settings screen reads as a set of color-coded category cards.
 */
private val SettingsSectionAccents = listOf(
    Color(0xFF8B5CF6), // violet
    Color(0xFF0EA5E9), // sky
    Color(0xFF14B8A6), // teal
    Color(0xFFF59E0B), // amber
    Color(0xFFF43F5E), // rose
    Color(0xFF10B981), // emerald
    Color(0xFFD946EF), // fuchsia
    Color(0xFFF97316), // orange
)

fun settingsGroupAccent(title: String?): Color {
    if (title == null) return SettingsSectionAccents.first()
    val idx = ((title.hashCode() % SettingsSectionAccents.size) + SettingsSectionAccents.size) % SettingsSectionAccents.size
    return SettingsSectionAccents[idx]
}

/**
 * A Material 3 Expressive style settings group component — JixOne 6:
 * color-coded category cards. Each group carries an accent (explicit or
 * derived from its title) that tints the section title and the leading icon
 * chips of its rows, turning the plain list into scannable colored blocks.
 * @param title The title of the settings group
 * @param items List of settings items to display
 * @param accent Optional explicit accent; defaults to a stable per-title hue
 */
@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    useLowContrast: Boolean = false,
    accent: Color? = null
) {
    // One accent for every group: the theme's own. A settings page is a
    // list to scan, not a rainbow.
    val groupAccent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        title?.let {
            Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp, top = 10.dp)
            )
        }

        // One solid card per group, rows split by hairlines. Solid, so the
        // text stays readable over a theme's background artwork.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .animateContentSize(),
        ) {
            items.forEachIndexed { index, item ->
                Material3SettingsItemRow(item = item, accent = groupAccent)
                if (index != items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(start = 72.dp, end = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

/**
 * Individual settings item row with Material 3 styling — JixOne 6: the
 * leading icon chip carries the group accent.
 */
@Composable
private fun Material3SettingsItemRow(
    item: Material3SettingsItem,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.enabled && item.onClick != null,
                onClick = { item.onClick?.invoke() }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom leading content or Icon with background
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(modifier = Modifier.width(16.dp))
        } else if (item.icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accent.copy(alpha = if (item.isHighlighted) 0.26f else 0.16f)),
                contentAlignment = Alignment.Center
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
                            tint = if (!item.enabled)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else
                                accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = if (!item.enabled)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
        }

        // Title and description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Title content
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    color = if (!item.enabled) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                item.title()
            }

            // Description if provided
            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = if (!item.enabled)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    desc()
                }
            }
        }

        // Trailing content, or a chevron on rows that open something
        if (item.trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            item.trailingContent.invoke()
        } else if (item.onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = androidx.compose.ui.res.painterResource(com.metrolist.music.R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Data class for Material 3 settings item
 */
data class Material3SettingsItem(
    val icon: Painter? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)
