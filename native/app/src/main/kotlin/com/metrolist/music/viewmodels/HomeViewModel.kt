/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.BrowseEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ShowWrappedCardKey
import com.metrolist.music.constants.WrappedSeenKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.GenreShelf
import com.metrolist.music.constants.OnboardingArtistGenresKey
import com.metrolist.music.ui.screens.onboarding.OnboardingSeedArtists
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.ui.screens.wrapped.WrappedAudioService
import com.metrolist.music.ui.screens.wrapped.WrappedManager
import com.metrolist.music.utils.NetworkConnectivityObserver
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    val wrappedManager: WrappedManager,
    private val wrappedAudioService: WrappedAudioService,
    private val networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)


    val explorePage = MutableStateFlow<ExplorePage?>(null)

    // JixOne: Explore personalization from the onboarding picks (saved as
    // bookmarked artists). Null = not loaded yet; empty = no picks made, so
    // the personalized sections simply stay hidden.
    val onboardingArtists = MutableStateFlow<List<com.metrolist.music.db.entities.Artist>?>(null)
    val onboardingRecommendations = MutableStateFlow<List<GenreShelf>?>(null)
    val onboardingSimilarArtists = MutableStateFlow<List<ArtistItem>?>(null)


    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())




    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

        val showWrappedCard: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        val showWrappedPref = prefs[ShowWrappedCardKey] ?: false
        val seen = prefs[WrappedSeenKey] ?: false
        val isBeforeDate = LocalDate.now().isBefore(LocalDate.of(2026, 2, 1))

        isBeforeDate && (!seen || showWrappedPref)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val wrappedSeen: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WrappedSeenKey] ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun markWrappedAsSeen() {
        viewModelScope.launch(Dispatchers.IO) {
            context.safeDataStoreEdit {
                it[WrappedSeenKey] = true
            }
        }
    }

    /**
     * JixOne: personalize Explore from the artists picked in the first-run
     * onboarding (persisted as bookmarked artists by OnboardingViewModel).
     * The picks always win the top of the page:
     *  1. a row of the picked artists themselves (selection order),
     *  2. one content shelf per picked artist — songs/videos off their page,
     *  3. a row of "Fans might also like" artists gathered from those pages.
     */
    private suspend fun getOnboardingRecommendations(
        hideExplicit: Boolean,
        hideVideoSongs: Boolean,
    ) {
        val picked = database.artistsBookmarkedByCreateDateAsc().first()
            .filter { it.artist.isYouTubeArtist }
        if (picked.isEmpty()) {
            onboardingArtists.value = emptyList()
            onboardingRecommendations.value = emptyList()
            onboardingSimilarArtists.value = emptyList()
            allYtItems.value = emptyList()
            return
        }
        onboardingArtists.value = picked

        val pickedIds = picked.map { it.id }.toSet()

        // Group the picks by genre so twenty artists become a handful of
        // shelves instead of twenty. Seeds carry their genre; other picks use
        // the genre saved at onboarding, or land in "other".
        val seedGenres = OnboardingSeedArtists.associate { it.id to it.genre }
        val savedGenres = decodeArtistGenres(context.dataStore.data.first()[OnboardingArtistGenresKey])
        val groups = LinkedHashMap<String, MutableList<Artist>>()
        // Newest picks first, so a fresh choice shows up at the top.
        picked.asReversed().forEach { artist ->
            val genre = savedGenres[artist.id] ?: seedGenres[artist.id] ?: GENRE_OTHER
            groups.getOrPut(genre) { mutableListOf() }.add(artist)
        }
        // "other" always goes last.
        groups.remove(GENRE_OTHER)?.let { groups[GENRE_OTHER] = it }

        // Same order for the whole day: a refresh must not reshuffle the page.
        val daySeed = LocalDate.now().toEpochDay()

        // A few artists per genre are enough to fill a shelf, and a cap on
        // the whole page keeps the number of requests sane.
        val toFetch = LinkedHashMap<String, Artist>()
        groups.values.forEach { artists ->
            artists.take(ARTISTS_PER_SHELF).forEach { toFetch.putIfAbsent(it.id, it) }
        }
        val fetchList = toFetch.values.take(MAX_ARTIST_FETCHES)

        val pages = coroutineScope {
            fetchList.map { artist ->
                async {
                    // YouTube.artist wraps failures in a Result, so one
                    // artist's failure cannot lose the rest.
                    artist.id to YouTube.artist(artist.id).getOrNull()
                }
            }.awaitAll()
        }.toMap()

        val similar = LinkedHashMap<String, ArtistItem>()
        pages.values.filterNotNull().forEach { page ->
            page.sections
                .firstNotNullOfOrNull { section ->
                    section.items.filterIsInstance<ArtistItem>().takeIf { it.isNotEmpty() }
                }
                ?.forEach { item ->
                    if (item.id !in pickedIds) similar.putIfAbsent(item.id, item)
                }
        }

        val shelves = groups.mapNotNull { (genre, artists) ->
            // Each artist's music, shuffled for today, then dealt round-robin
            // so no single artist owns the shelf.
            val perArtist = artists.mapNotNull { artist ->
                pages[artist.id]?.sections
                    ?.flatMap { it.items }
                    ?.filter { it !is ArtistItem }
                    ?.distinctBy { it.id }
                    ?.filterExplicit(hideExplicit)
                    ?.filterVideoSongs(hideVideoSongs)
                    ?.shuffled(Random(daySeed xor artist.id.hashCode().toLong()))
                    ?.takeIf { it.isNotEmpty() }
            }
            val mixed = mutableListOf<YTItem>()
            var index = 0
            while (mixed.size < ITEMS_PER_SHELF && perArtist.any { index < it.size }) {
                perArtist.forEach { list -> list.getOrNull(index)?.let { mixed += it } }
                index++
            }
            val items = mixed.distinctBy { it.id }.take(ITEMS_PER_SHELF)
            if (items.isEmpty()) null else GenreShelf(genre = genre, artists = artists, items = items)
        }

        // Every fetch failed (offline start): keep whatever is on screen and
        // let the next refresh / reconnect try again.
        val allFailed = pages.values.all { it == null }
        if (allFailed && !onboardingRecommendations.value.isNullOrEmpty()) return

        onboardingRecommendations.value = shelves
        onboardingSimilarArtists.value = similar.values.toList().take(20)

        // The Explore shuffle and the mix hero draw only from the personalized shelves.
        allYtItems.value = shelves.flatMap { it.items } + similar.values.toList()
    }

    /** Bumped by [refresh] to re-run the personalized fetches on demand. */
    private val personalizationReload = MutableStateFlow(0)

    /**
     * JixOne: keep the Explore personalization in sync with the picked
     * artists (stored as bookmarked artists). Watching the flow makes the
     * sections appear the moment the picks land in the database. The heavy
     * per-artist fetches re-run when the picked ID set changes, when the
     * content filters change, and on every refresh — the last one is what
     * recovers from an offline start once the network is back.
     */
    private fun startOnboardingWatcher() {
        viewModelScope.launch(Dispatchers.IO) {
            val pickedIds = database.artistsBookmarkedByCreateDateAsc()
                .map { artists -> artists.filter { it.artist.isYouTubeArtist }.map { it.id } }
                .distinctUntilChanged()
            val filters = context.dataStore.data
                .map { (it[HideExplicitKey] ?: false) to (it[HideVideoSongsKey] ?: false) }
                .distinctUntilChanged()
            combine(pickedIds, filters, personalizationReload) { _, f, _ -> f }
                .collectLatest { (hideExplicit, hideVideoSongs) ->
                    try {
                        getOnboardingRecommendations(hideExplicit, hideVideoSongs)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
        }
    }

    private suspend fun load() {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)

        // JixOne 6: Explore is 100% personalized — the only remote page we
        // need is the mood & genres list for manual browsing. The generic
        // YouTube home feed is gone for good.
        coroutineScope {
            launch(Dispatchers.IO) {
                YouTube.explore().onSuccess { page ->
                    explorePage.value = page.copy(
                        newReleaseAlbums = page.newReleaseAlbums.filterOutNulls().filterExplicit(hideExplicit),
                        moodAndGenres = page.moodAndGenres.filterOutNulls()
                    )
                }.onFailure { reportException(it) }
            }

            if (YouTube.cookie != null) {
                launch(Dispatchers.IO) { loadAccountInfo() }
            }
        }

        isLoading.value = false
    }

    private suspend fun loadAccountInfo() {
        YouTube.accountInfo().onSuccess { info ->
            accountName.value = info.name
            accountImageUrl.value = info.thumbnailUrl
        }.onFailure {
            reportException(it)
        }
    }


    /**
     * Safely filters out null items from a list whose type says non-null
     * but may contain nulls at runtime due to JSON parsing.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> List<T>.filterOutNulls(): List<T> =
        (this as List<T?>).filterNotNull()

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        personalizationReload.value++
        viewModelScope.launch(Dispatchers.IO) {
            try {
                load()
            } finally {
                isRefreshing.value = false
            }
        }
        // Run sync when user manually refreshes
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }
    }

    override fun onCleared() {
        super.onCleared()
        wrappedManager.dispose()
    }

    init {
        // Run sync in separate coroutine with cooldown to avoid blocking UI
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }

        var wasOffline = !networkConnectivity.networkStatus.value
        viewModelScope.launch(Dispatchers.IO) {
            networkConnectivity.networkStatus.collect { isConnected ->
                if (!isConnected) {
                    wasOffline = true
                } else if (wasOffline) {
                    wasOffline = false
                    refresh()
                }
            }
        }

        startOnboardingWatcher()

        // Prepare wrapped data in background
        viewModelScope.launch(Dispatchers.IO) {
            showWrappedCard.collect { shouldShow ->
                if (shouldShow && !wrappedManager.state.value.isDataReady) {
                    try {
                        wrappedManager.prepare()
                        val state = wrappedManager.state.first { it.isDataReady }
                        val trackMap = state.trackMap
                        if (trackMap.isNotEmpty()) {
                            val firstTrackId = trackMap.entries.first().value
                            wrappedAudioService.prepareTrack(firstTrackId)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
        }

        // Listen for cookie changes and reload account data
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] to it[AccountNameKey] }
                .distinctUntilChanged()
                .collect { (cookie, savedAccountName) ->
                    if (!cookie.isNullOrEmpty()) {
                        YouTube.cookie = cookie
                        accountName.value = savedAccountName.orEmpty().ifBlank { "Guest" }
                        loadAccountInfo()
                    } else {
                        accountName.value = "Guest"
                        accountImageUrl.value = null
                    }
                }
        }

    }

    private var isHomeDataLoaded = false

    private companion object {
        const val GENRE_OTHER = "other"
        const val ARTISTS_PER_SHELF = 4
        const val MAX_ARTIST_FETCHES = 16
        const val ITEMS_PER_SHELF = 18
    }

    fun loadHomeData() {
        if (isHomeDataLoaded) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cookie = context.dataStore.data
                    .map { it[InnerTubeCookieKey] }
                    .distinctUntilChanged()
                    .first()

                if (!cookie.isNullOrEmpty()) {
                    YouTube.cookie = cookie
                }

                isHomeDataLoaded = true
                load()
            } catch (e: Exception) {
                isHomeDataLoaded = false
                Timber.e(e, "Failed to load home data")
            }
        }
    }
}
