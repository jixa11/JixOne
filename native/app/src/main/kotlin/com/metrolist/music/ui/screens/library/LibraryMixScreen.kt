/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_PLAYLIST
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.MixSortDescendingKey
import com.metrolist.music.constants.MixSortType
import com.metrolist.music.constants.MixSortTypeKey
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowLikedPlaylistKey
import com.metrolist.music.constants.ShowTopPlaylistKey
import com.metrolist.music.constants.ShowUploadedPlaylistKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.extensions.reversed
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.AlbumListItem
import com.metrolist.music.ui.component.ArtistGridItem
import com.metrolist.music.ui.component.ArtistListItem
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySearchHeader
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.SongGridItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewType: LibraryViewType,
    onViewTypeChange: (LibraryViewType) -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val queueSearchedSongsStr = stringResource(R.string.queue_searched_songs)
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val (sortType, onSortTypeChange) =
        rememberEnumPreference(
            MixSortTypeKey,
            MixSortType.CREATE_DATE,
        )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsStateWithLifecycle()
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            }
        )
    }
    
    val normalizedQuery = remember(isSearchActive, searchQuery, debouncedSearchQuery) {
        if (isSearchActive) {
            searchQuery.normalizeForSearch()
        } else {
            debouncedSearchQuery.normalizeForSearch()
        }
    }

    val topSize by viewModel.topValue.collectAsStateWithLifecycle(initialValue = 50)
    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showUploaded) = rememberPreference(ShowUploadedPlaylistKey, true)

    // JixOne 4.1: live sync state + account + recently played for the shelf
    val syncState by viewModel.syncUiStateFlow.collectAsStateWithLifecycle()
    val recentSongs by viewModel.recentSongs.collectAsStateWithLifecycle()
    val (accountName) = rememberPreference(AccountNameKey, "")
    
    val albums = viewModel.albums.collectAsStateWithLifecycle()
    val artist = viewModel.artists.collectAsStateWithLifecycle()
    val songs = viewModel.songs.collectAsStateWithLifecycle()
    val playlist = viewModel.playlists.collectAsStateWithLifecycle()

    var allItems = albums.value + artist.value + playlist.value
    val locale = LocalLocale.current.platformLocale
    val collator = remember(locale) {
        Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
    }
    allItems =
        when (sortType) {
            MixSortType.CREATE_DATE -> {
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        is Song -> LocalDateTime.now()
                    }
                }
            }

            MixSortType.NAME -> {
                allItems.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            is Song -> ""
                        }
                    },
                )
            }

            MixSortType.LAST_UPDATED -> {
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        is Song -> LocalDateTime.now()
                    }
                }
            }
        }.reversed(sortDescending)

    val searchableItems = if (normalizedQuery.isBlank()) allItems else allItems + songs.value

    val filteredItems = remember(searchableItems, normalizedQuery, collator) {
        val matchedItems =
            searchableItems.filter { item ->
                when (item) {
                    is Song -> {
                        val artistNames = item.orderedArtists.map { it.name }.toTypedArray()
                        matchesNormalizedQuery(normalizedQuery, item.song.title, item.song.albumName, *artistNames)
                    }

                    is Album -> {
                        val artistNames = item.artists.map { it.name }.toTypedArray()
                        matchesNormalizedQuery(normalizedQuery, item.album.title, *artistNames)
                    }

                    is Artist -> matchesNormalizedQuery(normalizedQuery, item.artist.name)
                    is Playlist -> matchesNormalizedQuery(normalizedQuery, item.playlist.name)
                }
            }

        if (normalizedQuery.isBlank()) {
            matchedItems.distinctBy { it.id }
        } else {
            matchedItems
                .sortedWith { first, second ->
                    val firstPriority =
                        when (first) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                        }
                    val secondPriority =
                        when (second) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                        }

                    if (firstPriority != secondPriority) {
                        firstPriority.compareTo(secondPriority)
                    } else {
                        val firstName =
                            when (first) {
                                is Playlist -> first.playlist.name
                                is Song -> first.song.title
                                is Artist -> first.artist.name
                                is Album -> first.album.title
                            }
                        val secondName =
                            when (second) {
                                is Playlist -> second.playlist.name
                                is Song -> second.song.title
                                is Artist -> second.artist.name
                                is Album -> second.album.title
                            }
                        collator.compare(firstName, secondName)
                    }
                }
                .distinctBy { it.id }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.syncAllLibrary()
            }
        }
    }

    val headerContent = @Composable {
        LibrarySearchHeader(
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onBack = {
                isSearchActive = false
                viewModel.updateSearchQuery("")
            },
            keyboardController = keyboardController,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            Column {
                // JixOne 4.1: page identity row — bold Library title + actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.filter_library),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier.padding(start = 8.dp).size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.jix_search),
                            contentDescription = stringResource(R.string.search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(
                        onClick = {
                            onViewTypeChange(viewType.toggle())
                        },
                        modifier = Modifier.padding(end = 8.dp).size(40.dp),
                    ) {
                        Icon(
                            painter =
                            painterResource(
                                when (viewType) {
                                    LibraryViewType.LIST -> R.drawable.list
                                    LibraryViewType.GRID -> R.drawable.grid_view
                                },
                            ),
                            contentDescription = stringResource(
                                when (viewType) {
                                    LibraryViewType.LIST -> R.string.switch_to_grid_view
                                    LibraryViewType.GRID -> R.string.switch_to_list_view
                                },
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SortHeader(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = { sortType ->
                        when (sortType) {
                            MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                            MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                            MixSortType.NAME -> R.string.sort_by_name
                        }
                    },
                )
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                ),
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    // JixOne 4.1 Library — sync chip, recently played shelf and
                    // quick access tiles, above the library content
                    if (normalizedQuery.isBlank()) {
                        item(
                            key = "jix_sync_chip",
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            JixSyncChip(
                                accountName = accountName,
                                syncState = syncState,
                                onSync = { viewModel.refresh() },
                                onLogin = { navController.navigate("login") },
                            )
                        }

                        if (recentSongs.isNotEmpty()) {
                            item(
                                key = "jix_recent_shelf",
                                contentType = CONTENT_TYPE_HEADER,
                            ) {
                                val queueTitle = stringResource(R.string.recent_played)
                                JixRecentShelf(
                                    recentSongs = recentSongs,
                                    queueTitle = queueTitle,
                                    onPlay = { index ->
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = queueTitle,
                                                items = recentSongs.map { it.toMediaItem() },
                                                startIndex = index,
                                            ),
                                        )
                                    },
                                    onSeeAll = { navController.navigate("history") },
                                )
                            }
                        }

                        item(
                            key = "jix_quick_tiles",
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            JixQuickTiles(
                                showLiked = showLiked,
                                showDownloaded = showDownloaded,
                                showTop = showTop,
                                showCached = showCached,
                                showUploaded = showUploaded,
                                topSize = topSize.toString(),
                                onLiked = { navController.navigate("auto_playlist/liked") },
                                onDownloads = { navController.navigate("auto_playlist/downloaded") },
                                onTop = { navController.navigate("top_playlist/$topSize") },
                                onCached = { navController.navigate("cache_playlist/cached") },
                                onUploaded = { navController.navigate("auto_playlist/uploaded") },
                                onHistory = { navController.navigate("history") },
                            )
                        }
                    }

                    items(
                        items = filteredItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistListItem(
                                    playlist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (!item.playlist.isEditable && item.songCount == 0 &&
                                                        item.playlist.browseId != null
                                                    ) {
                                                        navController.navigate("online_playlist/${item.playlist.browseId}")
                                                    } else {
                                                        navController.navigate("local_playlist/${item.id}")
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        PlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }

                            is Song -> {
                                SongListItem(
                                    song = item,
                                    showInLibraryIcon = true,
                                    isActive = item.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = item,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (item.id == mediaMetadata?.id) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        val filteredSongs = filteredItems.filterIsInstance<Song>()
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = queueSearchedSongsStr,
                                                                items = filteredSongs.map { it.toMediaItem() },
                                                                startIndex = filteredSongs.indexOfFirst { it.id == item.id },
                                                            ),
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = item,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                )
                            }

                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }
                        }
                    }

                    if (filteredItems.isEmpty() && searchQuery.isNotBlank()) {
                        item(key = "empty_search_result") {
                            LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                        }
                    }
                }
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                        GridCells.Adaptive(
                            minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                        ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    // JixOne 4.1 Library — sync chip, recently played shelf and
                    // quick access tiles, flowing full-span above the grid
                    if (normalizedQuery.isBlank()) {
                        item(
                            key = "jix_sync_chip",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            JixSyncChip(
                                accountName = accountName,
                                syncState = syncState,
                                onSync = { viewModel.refresh() },
                                onLogin = { navController.navigate("login") },
                            )
                        }

                        if (recentSongs.isNotEmpty()) {
                            item(
                                key = "jix_recent_shelf",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = CONTENT_TYPE_HEADER,
                            ) {
                                val queueTitle = stringResource(R.string.recent_played)
                                JixRecentShelf(
                                    recentSongs = recentSongs,
                                    queueTitle = queueTitle,
                                    onPlay = { index ->
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = queueTitle,
                                                items = recentSongs.map { it.toMediaItem() },
                                                startIndex = index,
                                            ),
                                        )
                                    },
                                    onSeeAll = { navController.navigate("history") },
                                )
                            }
                        }

                        item(
                            key = "jix_quick_tiles",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            JixQuickTiles(
                                showLiked = showLiked,
                                showDownloaded = showDownloaded,
                                showTop = showTop,
                                showCached = showCached,
                                showUploaded = showUploaded,
                                topSize = topSize.toString(),
                                onLiked = { navController.navigate("auto_playlist/liked") },
                                onDownloads = { navController.navigate("auto_playlist/downloaded") },
                                onTop = { navController.navigate("top_playlist/$topSize") },
                                onCached = { navController.navigate("cache_playlist/cached") },
                                onUploaded = { navController.navigate("auto_playlist/uploaded") },
                                onHistory = { navController.navigate("history") },
                            )
                        }
                    }

                    items(
                        items = filteredItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistGridItem(
                                    playlist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (!item.playlist.isEditable && item.songCount == 0 &&
                                                        item.playlist.browseId != null
                                                    ) {
                                                        navController.navigate("online_playlist/${item.playlist.browseId}")
                                                    } else {
                                                        navController.navigate("local_playlist/${item.id}")
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        PlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }

                            is Song -> {
                                SongGridItem(
                                    song = item,
                                    showInLibraryIcon = true,
                                    isActive = item.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    fillMaxWidth = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (item.id == mediaMetadata?.id) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        val filteredSongs = filteredItems.filterIsInstance<Song>()
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = queueSearchedSongsStr,
                                                                items = filteredSongs.map { it.toMediaItem() },
                                                                startIndex = filteredSongs.indexOfFirst { it.id == item.id },
                                                            ),
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = item,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                )
                            }

                            is Artist -> {
                                ArtistGridItem(
                                    artist = item,
                                    fillMaxWidth = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumGridItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    fillMaxWidth = true,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }
                        }
                    }

                    if (filteredItems.isEmpty() && searchQuery.isNotBlank()) {
                        item(
                            key = "empty_search_result",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }

        // Always visible + button (no scroll hiding)
        FloatingActionButton(
            onClick = { showCreatePlaylistDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.jix_plus),
                contentDescription = stringResource(R.string.create_playlist),
            )
        }

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}


// ---------------------------------------------------------------------------
// JixOne 4.1 Library — sync chip, quick tiles, recently played shelf
// ---------------------------------------------------------------------------

/**
 * Account / live sync status pill. Signed in: idle -> tap to sync, running,
 * synced, failed -> tap to retry. Signed out: a sign-in hint that opens the
 * login screen. Makes YouTube playlist import visible instead of silent.
 */
@Composable
private fun JixSyncChip(
    accountName: String,
    syncState: SyncUtils.SyncUiState,
    onSync: () -> Unit,
    onLogin: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
            .clickable(onClick = if (accountName.isBlank()) onLogin else onSync)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        when {
            accountName.isBlank() -> {
                Icon(
                    painter = painterResource(R.drawable.jix_refresh),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.sync_sign_in_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            syncState is SyncUtils.SyncUiState.Running -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.syncing_library),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            syncState is SyncUtils.SyncUiState.Succeeded -> {
                Icon(
                    painter = painterResource(R.drawable.jix_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.synced_with_youtube),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            syncState is SyncUtils.SyncUiState.Failed -> {
                Icon(
                    painter = painterResource(R.drawable.jix_alert),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.sync_failed_retry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            else -> {
                Icon(
                    painter = painterResource(R.drawable.jix_refresh),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.sync_now),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Horizontal shelf of recently played songs with real album art and a
 * "See all" shortcut into the full history screen.
 */
@Composable
private fun JixRecentShelf(
    recentSongs: List<Song>,
    queueTitle: String,
    onPlay: (Int) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.recent_played),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.see_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(recentSongs, key = { it.id }) { song ->
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .width(112.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPlay(recentSongs.indexOfFirst { it.id == song.id }) },
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.orderedArtists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** One compact quick-access tile: tonal icon square + label. */
@Composable
private fun JixQuickTile(
    label: String,
    @DrawableRes icon: Int,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(84.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (accent) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(16.dp),
                ),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (accent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The quick-access row: Liked / Downloads / History / My top / Cached /
 * Uploaded — compact functional tiles, gated by the Library preferences.
 */
@Composable
private fun JixQuickTiles(
    showLiked: Boolean,
    showDownloaded: Boolean,
    showTop: Boolean,
    showCached: Boolean,
    showUploaded: Boolean,
    topSize: String,
    onLiked: () -> Unit,
    onDownloads: () -> Unit,
    onTop: () -> Unit,
    onCached: () -> Unit,
    onUploaded: () -> Unit,
    onHistory: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showLiked) {
            item(key = "tile_liked") {
                JixQuickTile(
                    label = stringResource(R.string.liked_songs),
                    icon = R.drawable.jix_heart_filled,
                    accent = true,
                    onClick = onLiked,
                )
            }
        }
        if (showDownloaded) {
            item(key = "tile_downloads") {
                JixQuickTile(
                    label = stringResource(R.string.downloads_label),
                    icon = R.drawable.jix_download,
                    accent = false,
                    onClick = onDownloads,
                )
            }
        }
        item(key = "tile_history") {
            JixQuickTile(
                label = stringResource(R.string.history),
                icon = R.drawable.jix_clock,
                accent = false,
                onClick = onHistory,
            )
        }
        if (showTop) {
            item(key = "tile_top") {
                JixQuickTile(
                    label = stringResource(R.string.my_top) + " $topSize",
                    icon = R.drawable.jix_trending,
                    accent = false,
                    onClick = onTop,
                )
            }
        }
        if (showCached) {
            item(key = "tile_cached") {
                JixQuickTile(
                    label = stringResource(R.string.cached_playlist),
                    icon = R.drawable.jix_database,
                    accent = false,
                    onClick = onCached,
                )
            }
        }
        if (showUploaded) {
            item(key = "tile_uploaded") {
                JixQuickTile(
                    label = stringResource(R.string.uploaded_playlist),
                    icon = R.drawable.jix_upload,
                    accent = false,
                    onClick = onUploaded,
                )
            }
        }
    }
}