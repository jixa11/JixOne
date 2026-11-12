/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_PLAYLIST
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.MixSortDescendingKey
import com.metrolist.music.constants.MixSortType
import com.metrolist.music.constants.MixSortTypeKey
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

    val topSize by viewModel.topValue.collectAsStateWithLifecycle(initialValue = "50")

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

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { isSearchActive = true },
                modifier = Modifier.padding(start = 8.dp).size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = stringResource(R.string.search),
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

                    if (normalizedQuery.isBlank()) {
                        item(
                            key = "jix_quick_cards",
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            JixLibraryShortcuts(
                                navController = navController,
                                topSize = topSize,
                                onCreatePlaylist = { showCreatePlaylistDialog = true },
                            )
                        }
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
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

                    // The shortcuts are one full-width block, so the collection
                    // grid below always starts on a fresh row.
                    if (normalizedQuery.isBlank()) {
                        item(
                            key = "jix_quick_cards",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            JixLibraryShortcuts(
                                navController = navController,
                                topSize = topSize,
                                onCreatePlaylist = { showCreatePlaylistDialog = true },
                            )
                        }
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
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


/**
 * Library shortcuts: two large gradient tiles for the places people open most
 * (liked songs, downloads), then one scrolling row of pills for the rest.
 */
@Composable
private fun JixLibraryShortcuts(
    navController: NavController,
    topSize: String,
    onCreatePlaylist: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            JixLibraryTile(
                label = stringResource(R.string.liked_songs),
                icon = R.drawable.jix_heart_filled,
                colors = listOf(scheme.primary, scheme.tertiary),
                onClick = { navController.navigate("auto_playlist/liked") },
                modifier = Modifier.weight(1f),
            )
            JixLibraryTile(
                label = stringResource(R.string.downloads_label),
                icon = R.drawable.jix_download,
                colors = listOf(scheme.tertiary, scheme.secondary),
                onClick = { navController.navigate("auto_playlist/downloaded") },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            JixLibraryPill(stringResource(R.string.new_playlist), R.drawable.jix_plus, onCreatePlaylist, highlighted = true)
            JixLibraryPill(stringResource(R.string.recent_played), R.drawable.jix_clock, { navController.navigate("history") })
            JixLibraryPill(stringResource(R.string.my_top) + " $topSize", R.drawable.trending_up, { navController.navigate("top_playlist/$topSize") })
            JixLibraryPill(stringResource(R.string.stats), R.drawable.stats, { navController.navigate("stats") })
            JixLibraryPill(stringResource(R.string.equalizer), R.drawable.equalizer, { navController.navigate("equalizer") })
            JixLibraryPill(stringResource(R.string.uploaded_playlist), R.drawable.upload, { navController.navigate("auto_playlist/uploaded") })
            JixLibraryPill(stringResource(R.string.cached_playlist), R.drawable.cached, { navController.navigate("cache_playlist/cached") })
        }
    }
}

/** A large gradient shortcut tile with a faded oversized icon. */
@Composable
private fun JixLibraryTile(
    label: String,
    @DrawableRes icon: Int,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    val ink =
        run {
            val lum = colors.map { it.luminance() }.average().toFloat()
            if (lum > 0.45f) Color.Black else Color.White
        }
    Box(
        modifier = modifier
            .height(104.dp)
            .shadow(elevation = 10.dp, shape = shape, ambientColor = colors.first(), spotColor = colors.first())
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = ink.copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp, y = 16.dp)
                .size(92.dp),
        )
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.18f)),
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = label,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A compact shortcut pill: tinted icon disc + label. */
@Composable
private fun JixLibraryPill(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    val shape = RoundedCornerShape(50)
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(46.dp)
            .clip(shape)
            .background(
                if (highlighted) scheme.primary.copy(alpha = 0.18f) else scheme.surfaceContainerHigh,
            )
            .border(
                1.dp,
                if (highlighted) scheme.primary.copy(alpha = 0.55f) else scheme.outlineVariant.copy(alpha = 0.35f),
                shape,
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.20f)),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            maxLines = 1,
        )
    }
}
