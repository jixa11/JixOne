/**
 * JixOne — Spotify-style artist onboarding.
 *
 * Pool model:
 *  - Start: curated verified seeds across many genres (see OnboardingSeeds).
 *  - Select artist X: every artist whose genre is outside X's genre family
 *    leaves the pool, and X's "Fans might also like" artists (fetched live
 *    from the artist page) join it — repeatedly, for every new selection.
 *  - Search box: rebuilds the list with live artist-search results first,
 *    then the already-selected artists (per the product spec).
 *  - Unlimited selections; "Done" subscribes/bookmarks everything picked.
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.YouTube.SearchFilter
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.ui.screens.onboarding.OnboardingSeedArtists
import com.metrolist.music.ui.screens.onboarding.SeedArtist
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** One artist tile in the onboarding grid. */
data class OnboardingArtist(
    val id: String,
    val name: String,
    val thumbnail: String?,
    /** Seed genre, or the genre inherited from the artist it was suggested by. */
    val genre: String?,
)

data class OnboardingUiState(
    /** Everything the grid shows right now (selected first). */
    val artists: List<OnboardingArtist> = emptyList(),
    val selected: List<OnboardingArtist> = emptyList(),
    val query: String = "",
    /** True while a search or similar-artists fetch is running. */
    val loading: Boolean = false,
    /** True once the seed pool has been built (instant — seeds are local). */
    val initialized: Boolean = false,
    /** Live artist-search results while typing (null = not searching). */
    val searchResults: List<OnboardingArtist>? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val database: MusicDatabase,
        private val syncUtils: SyncUtils,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState

        /** Seed pool, shuffled once, grouped by genre for the interleave. */
        private val seedsByGenre: Map<String, List<SeedArtist>> =
            OnboardingSeedArtists
                .distinctBy { it.id }
                .groupBy { it.genre }
                .mapValues { (_, list) -> list.shuffled() }

        /** Similar artists accumulated from selections (deduped). */
        private val similarArtists = LinkedHashMap<String, OnboardingArtist>()

        /** Candidate pool after the genre filter + similar merge. */
        private var currentPool: List<OnboardingArtist> = emptyList()

        private val queryFlow = MutableStateFlow("")
        private var searchJob: Job? = null

        init {
            rebuildPool()
            _uiState.value = _uiState.value.copy(initialized = true)

            // Debounced live search
            viewModelScope.launch {
                queryFlow.debounce(300).collect { q -> runSearch(q) }
            }
        }

        fun onQueryChange(query: String) {
            if (query == _uiState.value.query) return
            _uiState.value = _uiState.value.copy(query = query)
            queryFlow.value = query
            if (query.isBlank()) {
                searchJob?.cancel()
                _uiState.value = _uiState.value.copy(searchResults = null, loading = false)
                applyVisibility()
            }
        }

        fun toggleArtist(artist: OnboardingArtist) {
            val selected = _uiState.value.selected.toMutableList()
            if (selected.any { it.id == artist.id }) {
                selected.removeAll { it.id == artist.id }
                _uiState.value = _uiState.value.copy(selected = selected)
                rebuildPool()
            } else {
                selected.add(artist)
                _uiState.value = _uiState.value.copy(selected = selected, loading = true)
                fetchSimilar(artist)
            }
        }

        /**
         * Fetch the artist's "Fans might also like" circle and merge it into
         * the pool. Similar artists inherit the source artist's genre so the
         * genre filter keeps working for chained picks.
         */
        private fun fetchSimilar(artist: OnboardingArtist) {
            viewModelScope.launch {
                try {
                    val page = YouTube.artist(artist.id).getOrNull()
                    val similar =
                        page?.sections
                            ?.firstNotNullOfOrNull { section ->
                                section.items.filterIsInstance<ArtistItem>().takeIf { it.isNotEmpty() }
                            }.orEmpty()
                    similar.forEach { item ->
                        if (item.id != artist.id && !similarArtists.containsKey(item.id)) {
                            similarArtists[item.id] =
                                OnboardingArtist(
                                    id = item.id,
                                    name = item.title,
                                    thumbnail = item.thumbnail,
                                    genre = artist.genre,
                                )
                        }
                    }
                } catch (_: Exception) {
                    // Network hiccup: the pool simply keeps what it has.
                } finally {
                    rebuildPool()
                    _uiState.value = _uiState.value.copy(loading = false)
                }
            }
        }

        /**
         * Rebuild the candidate pool:
         *  - seeds only from genres the user has picked (all genres when
         *    nothing is selected yet),
         *  - plus every similar artist discovered so far,
         *  - minus the already-selected artists.
         */
        private fun rebuildPool() {
            val state = _uiState.value
            val selectedIds = state.selected.map { it.id }.toSet()
            val activeGenres =
                state.selected.mapNotNull { it.genre }.toSet().ifEmpty { null }

            val seedPool =
                (
                    if (activeGenres == null) {
                        seedsByGenre.values
                    } else {
                        seedsByGenre.filterKeys { it in activeGenres }.values
                    }
                ).flatten()
                    .filter { it.id !in selectedIds }
                    .map { OnboardingArtist(it.id, it.name, it.thumbnail, it.genre) }

            val similarPool = similarArtists.values.filter { it.id !in selectedIds }

            // Similar artists first (freshest signal), then remaining seeds.
            currentPool = similarPool + seedPool
            applyVisibility()
        }

        /** Decide what the grid shows: search mode or pool mode. */
        private fun applyVisibility() {
            val state = _uiState.value
            val artists =
                if (state.query.isNotBlank() && state.searchResults != null) {
                    // Search results first, then selected artists not in them.
                    val results = state.searchResults
                    val selectedNotInResults = state.selected.filter { s -> results.none { it.id == s.id } }
                    results + selectedNotInResults
                } else {
                    // Selected pinned on top, then the genre/similar pool.
                    val poolNotSelected = currentPool.filter { p -> state.selected.none { it.id == p.id } }
                    state.selected + poolNotSelected
                }
            _uiState.value = state.copy(artists = artists.distinctBy { it.id })
        }

        private fun runSearch(query: String) {
            if (query.isBlank()) return
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(loading = true)
                    try {
                        val results =
                            YouTube
                                .search(query, SearchFilter.FILTER_ARTIST)
                                .getOrNull()
                                ?.items
                                ?.filterIsInstance<ArtistItem>()
                                .orEmpty()
                                .map { OnboardingArtist(it.id, it.title, it.thumbnail, genre = null) }
                        _uiState.value = _uiState.value.copy(searchResults = results)
                    } catch (_: Exception) {
                        _uiState.value = _uiState.value.copy(searchResults = emptyList())
                    } finally {
                        _uiState.value = _uiState.value.copy(loading = false)
                        applyVisibility()
                    }
                }
        }

        /**
         * Persist the picks: bookmark every selected artist locally and sync
         * the subscription to the signed-in YouTube Music account (no-op
         * when signed out — handled inside SyncUtils). Reports how many
         * artists were saved.
         */
        fun saveSelections(onSaved: (Int) -> Unit) {
            val selected = _uiState.value.selected
            if (selected.isEmpty()) {
                onSaved(0)
                return
            }
            viewModelScope.launch {
                var saved = 0
                selected.forEach { artist ->
                    try {
                        val existing = database.artistEntity(artist.id)
                        if (existing != null) {
                            if (existing.bookmarkedAt == null) {
                                database.update(
                                    existing.copy(
                                        name = artist.name,
                                        thumbnailUrl = artist.thumbnail ?: existing.thumbnailUrl,
                                        bookmarkedAt = LocalDateTime.now(),
                                    ),
                                )
                            }
                        } else {
                            database.insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.name,
                                    thumbnailUrl = artist.thumbnail,
                                    channelId = artist.id,
                                    bookmarkedAt = LocalDateTime.now(),
                                ),
                            )
                        }
                        syncUtils.subscribeChannel(artist.id, true)
                        saved++
                    } catch (_: Exception) {
                        // Keep going: one failure must not lose the rest.
                    }
                }
                onSaved(saved)
            }
        }
    }
