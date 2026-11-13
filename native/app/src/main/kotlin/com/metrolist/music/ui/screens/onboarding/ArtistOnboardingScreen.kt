/**
 * JixOne — Spotify-style artist onboarding.
 *
 * Big avatar tiles across many genres → pick artists → the pool narrows to
 * the picked genres and refills with "Fans might also like" suggestions;
 * a search box above the grid rebuilds it with live results (selected
 * artists always stay reachable). Unlimited picks, "Done" saves them all.
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.theme.bbhBartle
import com.metrolist.music.viewmodels.OnboardingArtist
import com.metrolist.music.viewmodels.OnboardingViewModel

@Composable
fun ArtistOnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val insets = LocalPlayerAwareWindowInsets.current

    val selectedCountLabel = stringResource(R.string.onboarding_selected_count, uiState.selected.size)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal)),
    ) {
        // ---------- header + search ----------
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(insets.only(WindowInsetsSides.Top))
                    .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                fontFamily = bbhBartle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(stringResource(R.string.onboarding_search_hint))
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.jix_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                painter = painterResource(R.drawable.jix_x),
                                contentDescription = stringResource(R.string.clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (uiState.loading) {
                        com.metrolist.music.ui.jix.JixLoader(height = 20.dp)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
            )

            Spacer(Modifier.height(4.dp))

            // status line: how many picked so far
            AnimatedVisibility(visible = uiState.selected.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = selectedCountLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }

        // ---------- artist grid ----------
        Box(modifier = Modifier.weight(1f)) {
            if (!uiState.initialized) {
                com.metrolist.music.ui.jix.JixLoader(height = 34.dp)
            } else if (uiState.artists.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search_off),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 16.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(uiState.artists, key = { it.id }) { artist ->
                        ArtistTile(
                            artist = artist,
                            selected = uiState.selected.any { it.id == artist.id },
                            onClick = { viewModel.toggleArtist(artist) },
                        )
                    }
                }
            }
        }

        // ---------- bottom bar: done ----------
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 8.dp,
        ) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(insets.only(WindowInsetsSides.Bottom))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedCountLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.selected.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            // The ViewModel persists the picks and the
                            // "completed" flag before calling back.
                            viewModel.saveSelections { _ -> onFinished() }
                        },
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_done),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistTile(
    artist: OnboardingArtist,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(artist.thumbnail)
                        .crossfade(true)
                        .build(),
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (selected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(18.dp),
                                )
                            } else {
                                Modifier
                            },
                        ),
            )
            // Spotify-style check badge on the selected tile
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = artist.name,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
