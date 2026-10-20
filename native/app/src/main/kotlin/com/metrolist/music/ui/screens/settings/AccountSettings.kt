/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.UseLoginForBrowse
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.InfoLabel
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AccountSettingsViewModel
import com.metrolist.music.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * JixOne account sheet — the modern profile menu. Replaces the old
 * Metrolist-style dialog list: a compact avatar header, a quick-action
 * grid that consolidates the former top-bar icons (history, stats,
 * listen-together, settings...), and clean account/sync rows.
 * Everything is themed through MaterialTheme so all JixOne themes
 * (incl. Persian artwork) stay harmonious.
 */
@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (authUser, onAuthUserChange) = rememberPreference(InnerTubeAuthUserKey, "0")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // rotating tonal hues for the quick grid — always harmonious with the theme
    val chipColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
    )

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 14.dp),
    ) {
        // ---------- header ----------
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                if (isLoggedIn && accountImageUrl != null) {
                    AsyncImage(
                        model = accountImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    )
                                )
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.jix_user_round),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) (accountName.ifBlank { stringResource(R.string.account) })
                    else stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isLoggedIn) {
                    Text(
                        text = accountEmail.ifBlank { accountChannelHandle },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.yt_account_connected),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.yt_account_signed_out),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TextButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.jix_close_bold),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // sign-in CTA when logged out
        if (!isLoggedIn) {
            Spacer(Modifier.height(14.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            )
                        )
                    )
                    .clickable {
                        onClose()
                        navController.navigate("login")
                    }
                    .padding(vertical = 13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.jix_log_in),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.login),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }

        // ---------- quick action grid (former top-bar icons, consolidated) ----------
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.quick_actions),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )

        val updateAvailable = BuildConfig.UPDATER_AVAILABLE &&
            latestVersionName != BuildConfig.BASE_VERSION_NAME

        data class QuickAction(
            val label: String,
            val icon: Painter,
            val route: String,
            val badge: Boolean = false,
        )

        val quickActions = listOfNotNull(
            QuickAction(stringResource(R.string.history), painterResource(R.drawable.jix_history), "history"),
            QuickAction(stringResource(R.string.stats), painterResource(R.drawable.jix_activity), "stats"),
            QuickAction(stringResource(R.string.together), painterResource(R.drawable.jix_users), "listen_together_from_topbar"),
            QuickAction(stringResource(R.string.equalizer), painterResource(R.drawable.jix_sliders), "equalizer"),
            QuickAction(stringResource(R.string.backup_restore), painterResource(R.drawable.jix_backup), "settings/backup_restore"),
            QuickAction(stringResource(R.string.settings), painterResource(R.drawable.jix_settings), "settings", badge = updateAvailable),
            QuickAction(stringResource(R.string.about), painterResource(R.drawable.jix_info), "settings/about"),
            QuickAction(stringResource(R.string.integrations), painterResource(R.drawable.jix_link), "settings/integrations"),
        )

        quickActions.chunked(4).forEach { rowActions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                rowActions.forEachIndexed { i, action ->
                    val chipColor = chipColors[(quickActions.indexOf(action)) % chipColors.size]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable {
                                onClose()
                                navController.navigate(action.route)
                            }
                            .padding(vertical = 12.dp),
                    ) {
                        if (action.badge) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 10.dp)
                                    .size(7.dp),
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(chipColor.copy(alpha = 0.14f)),
                            ) {
                                Icon(
                                    painter = action.icon,
                                    contentDescription = action.label,
                                    tint = chipColor,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = action.label,
                                fontSize = 10.5.sp,
                                lineHeight = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                // pad incomplete rows so cells keep equal width
                repeat(4 - rowActions.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // ---------- account & sync section ----------
        Spacer(Modifier.height(8.dp))

        if (isLoggedIn) {
            AccountRow(
                icon = painterResource(R.drawable.jix_users),
                title = stringResource(R.string.switch_youtube_channel),
                onClick = {
                    onClose()
                    navController.navigate("switch_channel")
                },
            )
            AccountRow(
                icon = painterResource(R.drawable.jix_key),
                title = when {
                    !isLoggedIn -> stringResource(R.string.advanced_login)
                    showToken -> stringResource(R.string.token_shown)
                    else -> stringResource(R.string.token_hidden)
                },
                onClick = {
                    if (!isLoggedIn) showTokenEditor = true
                    else if (!showToken) showToken = true
                    else showTokenEditor = true
                },
            )
            AccountSwitchRow(
                icon = painterResource(R.drawable.jix_globe),
                title = stringResource(R.string.more_content),
                checked = useLoginForBrowse,
                enabled = isLoggedIn,
                onCheckedChange = {
                    YouTube.useLoginForBrowse = it
                    onUseLoginForBrowseChange(it)
                },
            )
            AccountSwitchRow(
                icon = painterResource(R.drawable.jix_refresh),
                title = stringResource(R.string.yt_sync),
                checked = ytmSync,
                enabled = isLoggedIn,
                onCheckedChange = onYtmSyncChange,
            )
        } else {
            AccountRow(
                icon = painterResource(R.drawable.jix_key),
                title = stringResource(R.string.advanced_login),
                onClick = { showTokenEditor = true },
            )
        }

        // ---------- update banner ----------
        if (updateAvailable) {
            val releaseInfo = Updater.getCachedLatestRelease()
            val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }
            if (downloadUrl != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { uriHandler.openUri(downloadUrl) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.jix_update),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.new_version_available),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = latestVersionName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---------- logout ----------
        if (isLoggedIn) {
            Spacer(Modifier.height(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                    .clickable { showLogoutDialog = true }
                    .padding(vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.jix_log_out),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_logout),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        // ---------- dialogs (logic unchanged from the previous design) ----------
        if (showLogoutDialog) {
            DefaultDialog(
                onDismiss = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.logout_dialog_title)) },
                content = {
                    Text(
                        text = stringResource(R.string.logout_dialog_message),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                },
                buttons = {
                    TextButton(
                        onClick = {
                            Timber.d("[LOGOUT_CLEAR] User chose to clear data")
                            scope.launch {
                                try {
                                    accountSettingsViewModel.logoutAndClearLibraryData(context)
                                } catch (e: Exception) {
                                    Timber.e(e, "[LOGOUT_CLEAR] Error clearing library data")
                                    reportException(e)
                                }
                                onInnerTubeCookieChange("")
                                showLogoutDialog = false
                                onClose()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.logout_clear))
                    }
                    TextButton(
                        onClick = {
                            Timber.d("[LOGOUT_KEEP] User chose to keep data")
                            scope.launch {
                                accountSettingsViewModel.logoutKeepData(context, onInnerTubeCookieChange)
                                showLogoutDialog = false
                                onClose()
                            }
                        },
                    ) {
                        Text(stringResource(R.string.logout_keep))
                    }
                },
            )
        }

        if (showTokenEditor) {
            val text = """
                ***INNERTUBE COOKIE*** =$innerTubeCookie
                ***VISITOR DATA*** =$visitorData
                ***DATASYNC ID*** =$dataSyncId
                ***AUTH USER*** =$authUser
                ***ACCOUNT NAME*** =$accountNamePref
                ***ACCOUNT EMAIL*** =$accountEmail
                ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
            """.trimIndent()

            TextFieldDialog(
                initialTextFieldValue = TextFieldValue(text),
                onDone = { data ->
                    var cookie = ""
                    var visitorDataValue = ""
                    var dataSyncIdValue = ""
                    var authUserValue = "0"
                    var accountNameValue = ""
                    var accountEmailValue = ""
                    var accountChannelHandleValue = ""

                    data.split("\n").forEach {
                        when {
                            it.startsWith("***INNERTUBE COOKIE*** =") -> cookie = it.substringAfter("=")
                            it.startsWith("***VISITOR DATA*** =") -> visitorDataValue = it.substringAfter("=")
                            it.startsWith("***DATASYNC ID*** =") -> dataSyncIdValue = it.substringAfter("=")
                            it.startsWith("***AUTH USER*** =") -> authUserValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT NAME*** =") -> accountNameValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT EMAIL*** =") -> accountEmailValue = it.substringAfter("=")
                            it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> accountChannelHandleValue = it.substringAfter("=")
                        }
                    }
                    accountSettingsViewModel.saveTokenAndRestart(
                        context = context,
                        cookie = cookie,
                        visitorData = visitorDataValue,
                        dataSyncId = dataSyncIdValue,
                        authUser = authUserValue,
                        accountName = accountNameValue,
                        accountEmail = accountEmailValue,
                        accountChannelHandle = accountChannelHandleValue,
                    )
                },
                onDismiss = { showTokenEditor = false },
                singleLine = false,
                maxLines = 20,
                isInputValid = { fullText ->
                    val cookieLine = fullText.lines()
                        .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                    val cookieValue = cookieLine?.substringAfter("***INNERTUBE COOKIE*** =")?.trim() ?: ""
                    cookieValue.isNotEmpty() && "SAPISID" in parseCookieString(cookieValue)
                },
                extraContent = {
                    Spacer(Modifier.height(8.dp))
                    InfoLabel(text = stringResource(R.string.token_adv_login_description))
                },
            )
        }
    }
}

@Composable
private fun AccountRow(
    icon: Painter,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.jix_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AccountSwitchRow(
    icon: Painter,
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.weight(1f),
        )
        Switch(
            enabled = enabled,
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}
