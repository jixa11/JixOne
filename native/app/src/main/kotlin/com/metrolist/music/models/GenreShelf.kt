package com.metrolist.music.models

import com.metrolist.innertube.models.YTItem
import com.metrolist.music.db.entities.Artist

/** One Explore shelf: a genre, the picked artists in it, and a mix of their music. */
data class GenreShelf(
    val genre: String,
    val artists: List<Artist>,
    val items: List<YTItem>,
)
