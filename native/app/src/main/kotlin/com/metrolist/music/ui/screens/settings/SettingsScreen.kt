/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalChangelogState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.ReleaseNotesCard
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.Updater

/**
 * JixOne modern settings — the web app's clean grouped settings on the
 * native engine: a compact hero header, one soft card per group, tonal
 * icon chips in rotating theme hues, and a description under every row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo(
                "com.google.android.projection.gearhead", 0
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        // ---------- hero header ----------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp, start = 4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.small_icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(42.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = stringResource(R.string.app_name) + " " + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------- look & feel ----------
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.jix_palette),
                    iconTint = primary,
                    title = { Text(stringResource(R.string.appearance)) },
                    description = { Text(stringResource(R.string.settings_desc_appearance)) },
                    onClick = { navController.navigate("settings/appearance") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.jix_headphones),
                    iconTint = tertiary,
                    title = { Text(stringResource(R.string.player_and_audio)) },
                    description = { Text(stringResource(R.string.settings_desc_player)) },
                    onClick = { navController.navigate("settings/player") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.jix_globe),
                    iconTint = secondary,
                    title = { Text(stringResource(R.string.content)) },
                    description = { Text(stringResource(R.string.settings_desc_content)) },
                    onClick = { navController.navigate("settings/content") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.jix_sparkles),
                    iconTint = primary,
                    title = { Text(stringResource(R.string.ai_lyrics_translation)) },
                    description = { Text(stringResource(R.string.settings_desc_ai)) },
                    onClick = { navController.navigate("settings/ai") }
                ),
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- account & data ----------
        if (hasAndroidAuto) {
            Material3SettingsGroup(
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.jix_car),
                        iconTint = tertiary,
                        title = { Text(stringResource(R.string.android_auto)) },
                        description = { Text(stringResource(R.string.settings_desc_auto)) },
                        onClick = { navController.navigate("settings/android_auto") }
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.jix_shield),
                    iconTint = secondary,
                    title = { Text(stringResource(R.string.privacy)) },
                    description = { Text(stringResource(R.string.settings_desc_privacy)) },
                    onClick = { navController.navigate("settings/privacy") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.jix_folder),
                    iconTint = tertiary,
                    title = { Text(stringResource(R.string.storage)) },
                    description = { Text(stringResource(R.string.settings_desc_storage)) },
                    onClick = { navController.navigate("settings/storage") }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.jix_backup),
                    iconTint = primary,
                    title = { Text(stringResource(R.string.backup_restore)) },
                    description = { Text(stringResource(R.string.settings_desc_backup)) },
                    onClick = { navController.navigate("settings/backup_restore") }
                ),
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- system ----------
        Material3SettingsGroup(
            items = buildList {
                if (isAndroid12OrLater) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.jix_link),
                            iconTint = secondary,
                            title = { Text(stringResource(R.string.default_links)) },
                            description = { Text(stringResource(R.string.settings_desc_links)) },
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                        "package:${context.packageName}".toUri()
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        R.string.open_app_settings_error,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    )
                }
                if (BuildConfig.UPDATER_AVAILABLE) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.jix_update),
                            iconTint = primary,
                            title = { Text(stringResource(R.string.updater)) },
                            description = { Text(stringResource(R.string.settings_desc_updater)) },
                            onClick = { navController.navigate("settings/updater") }
                        )
                    )
                }
                val showChangelog = LocalChangelogState.current
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.jix_newspaper),
                        iconTint = tertiary,
                        title = { Text(stringResource(R.string.changelog)) },
                        description = { Text(stringResource(R.string.settings_desc_changelog)) },
                        onClick = { showChangelog.value = true }
                    )
                )
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.jix_info),
                        iconTint = secondary,
                        title = { Text(stringResource(R.string.about)) },
                        description = { Text(stringResource(R.string.settings_desc_about)) },
                        onClick = { navController.navigate("settings/about") }
                    )
                )
                if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.BASE_VERSION_NAME) {
                    val releaseInfo = Updater.getCachedLatestRelease()
                    val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }

                    if (downloadUrl != null) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.jix_update),
                                iconTint = MaterialTheme.colorScheme.error,
                                title = {
                                    Text(
                                        text = stringResource(R.string.new_version_available),
                                    )
                                },
                                description = {
                                    Text(
                                        text = latestVersionName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                showBadge = true,
                                onClick = { uriHandler.openUri(downloadUrl) }
                            )
                        )
                    }
                }
            },
        )

        if (BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.BASE_VERSION_NAME) {
            Spacer(modifier = Modifier.height(16.dp))
            ReleaseNotesCard()
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.jix_arrow_left),
                    contentDescription = null
                )
            }
        }
    )
}
