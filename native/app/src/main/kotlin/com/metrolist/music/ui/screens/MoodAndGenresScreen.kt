/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val localConfiguration = LocalConfiguration.current
    val itemsPerRow = if (localConfiguration.orientation == ORIENTATION_LANDSCAPE) 3 else 2

    val moodAndGenresList by viewModel.moodAndGenres.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        if (moodAndGenresList == null) {
            item(key = "mood_and_genres_shimmer") {
                ShimmerHost(
                    modifier = Modifier.animateItem()
                ) {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }

        moodAndGenresList?.forEachIndexed { index, moodAndGenres ->
            item(key = "mood_and_genres_section_$index") {
                Column(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 6.dp),
                ) {
                    NavigationTitle(
                        title = moodAndGenres.title,
                    )
                    moodAndGenres.items.chunked(itemsPerRow).forEach { row ->
                        Row {
                            row.forEach {
                                MoodAndGenresButton(
                                    title = it.title,
                                    onClick = {
                                        navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                                    },
                                    modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(6.dp),
                                )
                            }

                            repeat(itemsPerRow - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.mood_and_genres)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

//
// JixOne 6: the mood chips get a full makeover — every mood is a vibrant
// gradient tile with its own color identity (matched by keyword, with a
// stable-hash fallback for anything unrecognized) instead of the flat gray
// rectangles of the old design.
//

private data class MoodPalette(
    val top: Color,
    val bottom: Color,
)

private val moodKeywords: List<Pair<String, MoodPalette>> = listOf(
    "autumn" to MoodPalette(Color(0xFFFF9A3D), Color(0xFFB8500A)),
    "fall" to MoodPalette(Color(0xFFFF9A3D), Color(0xFFB8500A)),
    "chill" to MoodPalette(Color(0xFF3ED8E5), Color(0xFF1B6BC9)),
    "energize" to MoodPalette(Color(0xFFFF512F), Color(0xFFC11D67)),
    "energy" to MoodPalette(Color(0xFFFF512F), Color(0xFFC11D67)),
    "workout" to MoodPalette(Color(0xFFFF416C), Color(0xFFA82247)),
    "gym" to MoodPalette(Color(0xFFFF416C), Color(0xFFA82247)),
    "feel-good" to MoodPalette(Color(0xFFFFC93C), Color(0xFFF07F00)),
    "feel good" to MoodPalette(Color(0xFFFFC93C), Color(0xFFF07F00)),
    "happy" to MoodPalette(Color(0xFFFFC93C), Color(0xFFF07F00)),
    "christmas" to MoodPalette(Color(0xFF19B08A), Color(0xFF0B6E56)),
    "holiday" to MoodPalette(Color(0xFF19B08A), Color(0xFF0B6E56)),
    "focus" to MoodPalette(Color(0xFF5A60E8), Color(0xFF2A2FA0)),
    "study" to MoodPalette(Color(0xFF5A60E8), Color(0xFF2A2FA0)),
    "concentration" to MoodPalette(Color(0xFF5A60E8), Color(0xFF2A2FA0)),
    "commute" to MoodPalette(Color(0xFF8A5CFF), Color(0xFF4B23B8)),
    "driving" to MoodPalette(Color(0xFF8A5CFF), Color(0xFF4B23B8)),
    "gaming" to MoodPalette(Color(0xFF7F00FF), Color(0xFFC100D0)),
    "romance" to MoodPalette(Color(0xFFF857A6), Color(0xFFB02D6E)),
    "love" to MoodPalette(Color(0xFFF857A6), Color(0xFFB02D6E)),
    "relax" to MoodPalette(Color(0xFF3BC9A5), Color(0xFF147A70)),
    "sleep" to MoodPalette(Color(0xFF4A6FA5), Color(0xFF2C3E6B)),
    "party" to MoodPalette(Color(0xFFF857A6), Color(0xFF9D2B7A)),
    "dance" to MoodPalette(Color(0xFFF857A6), Color(0xFF9D2B7A)),
    "sad" to MoodPalette(Color(0xFF5C7A99), Color(0xFF33475C)),
    "pop" to MoodPalette(Color(0xFFFF5F6D), Color(0xFFC33764)),
    "rock" to MoodPalette(Color(0xFF6D7B8D), Color(0xFF2F3B4C)),
    "metal" to MoodPalette(Color(0xFF5A5F6B), Color(0xFF232733)),
    "hip-hop" to MoodPalette(Color(0xFFF2B136), Color(0xFF93520E)),
    "hip hop" to MoodPalette(Color(0xFFF2B136), Color(0xFF93520E)),
    "rap" to MoodPalette(Color(0xFFF2B136), Color(0xFF93520E)),
    "jazz" to MoodPalette(Color(0xFFF8A534), Color(0xFF9C5410)),
    "soul" to MoodPalette(Color(0xFFD9805F), Color(0xFF8C4632)),
    "country" to MoodPalette(Color(0xFFC98A4B), Color(0xFF7A4A1D)),
    "folk" to MoodPalette(Color(0xFFB5A067), Color(0xFF6B5B2E)),
    "latin" to MoodPalette(Color(0xFFFF6A3D), Color(0xFFB32E0E)),
    "edm" to MoodPalette(Color(0xFF00C9FF), Color(0xFF7A00FF)),
    "electronic" to MoodPalette(Color(0xFF00C9FF), Color(0xFF7A00FF)),
    "house" to MoodPalette(Color(0xFF00C9FF), Color(0xFF7A00FF)),
    "techno" to MoodPalette(Color(0xFF00C9FF), Color(0xFF7A00FF)),
    "r&b" to MoodPalette(Color(0xFFCB4B6E), Color(0xFF7A2444)),
    "classical" to MoodPalette(Color(0xFF9B6FB8), Color(0xFF4F2D6B)),
    "indie" to MoodPalette(Color(0xFF40C9B7), Color(0xFF5B4FB8)),
    "k-pop" to MoodPalette(Color(0xFFFF6FD8), Color(0xFF8F2FBF)),
    "worship" to MoodPalette(Color(0xFF7FA8D9), Color(0xFF3E5C8F)),
)

private val moodFallbacks = listOf(
    MoodPalette(Color(0xFF7C5CFF), Color(0xFF3D2BA8)),
    MoodPalette(Color(0xFF00C9FF), Color(0xFF0072FF)),
    MoodPalette(Color(0xFFFF8A3D), Color(0xFFC44B00)),
    MoodPalette(Color(0xFF43CEA2), Color(0xFF1D8A87)),
    MoodPalette(Color(0xFFF857A6), Color(0xFFC4457F)),
    MoodPalette(Color(0xFFDA97FF), Color(0xFF7B2FBF)),
)

internal fun moodPaletteFor(title: String): MoodPalette {
    val lower = title.lowercase()
    moodKeywords.firstOrNull { (k, _) -> k in lower }?.let { return it.second }
    val idx = ((title.hashCode() % moodFallbacks.size) + moodFallbacks.size) % moodFallbacks.size
    return moodFallbacks[idx]
}

@Composable
fun MoodAndGenresButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = remember(title) { moodPaletteFor(title) }
    Box(
        modifier =
        modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(palette.top, palette.bottom),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        // Decorative glow: a large translucent circle clipped to the corner,
        // the signature look of the new JixOne tiles.
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 34.dp)
                    .size(72.dp)
                    .alpha(0.22f)
                    .background(Color.White, CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 62.dp)
                    .size(40.dp)
                    .alpha(0.16f)
                    .background(Color.White, CircleShape),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

val MoodAndGenresButtonHeight = 52.dp
