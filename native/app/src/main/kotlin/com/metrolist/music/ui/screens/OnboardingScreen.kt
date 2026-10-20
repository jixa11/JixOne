/**
 * JixOne 5.0 — first-install onboarding (Spotify-style):
 * step 1 = language (fa/en/system), step 2 = pick the moods & genres you
 * love; the choices personalize the Explore tab ("For you" shelf).
 * Fresh installs only — updaters never see it (MainActivity gates on an
 * empty database).
 */

package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.OnboardingCompletedKey
import com.metrolist.music.constants.OnboardingInterestsKey
import com.metrolist.music.constants.UiLanguageKey
import com.metrolist.music.ui.theme.MetrolistTheme
import com.metrolist.music.utils.safeDataStoreEdit
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.utils.dataStore
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.MoodAndGenres

/** interests serialization: "browseId\u0001params\u0001title" joined by \u0002 */
private const val FIELD = '\u0001'
private const val ENTRY = '\u0002'

fun serializeInterests(items: List<MoodAndGenres.Item>): String =
    items.joinToString(ENTRY.toString()) { "${it.endpoint.browseId}$FIELD${it.endpoint.params ?: ""}$FIELD${it.title}" }

fun deserializeInterests(raw: String): List<Triple<String, String, String>> =
    raw.split(ENTRY)
        .filter { it.isNotBlank() }
        .mapNotNull { entry ->
            val parts = entry.split(FIELD)
            if (parts.size == 3) Triple(parts[0], parts[1], parts[2]) else null
        }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (uiLanguage, onUiLanguageChange) = rememberPreference(UiLanguageKey, "system")
    var step by rememberSaveable { mutableStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }

    fun finish(interestsRaw: String) {
        navController.navigate("home") {
            popUpTo("onboarding") { inclusive = true }
        }
    }

    BackHandler(enabled = step == 1) { step = 0 }

    val insets = WindowInsets.systemBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = insets.calculateTopPadding(),
                    bottom = insets.calculateBottomPadding(),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // top row: dots + skip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(width = if (step == index) 22.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (step == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    scope.launch {
                        context.safeDataStoreEdit { it[OnboardingCompletedKey] = true }
                    }
                    finish("")
                }) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 3 } + fadeOut()
                    }
                },
                label = "onboardingSteps",
            ) { currentStep ->
                when (currentStep) {
                    0 -> WelcomeStep(
                        uiLanguage = uiLanguage,
                        onUiLanguageChange = onUiLanguageChange,
                        onContinue = { step = 1 },
                    )

                    else -> InterestsStep(
                        selected = selected,
                        onToggle = { key ->
                            selected = if (key in selected) {
                                selected - key
                            } else {
                                selected + key
                            }
                        },
                        onDone = { interests ->
                            scope.launch {
                                context.safeDataStoreEdit { settings ->
                                    settings[OnboardingInterestsKey] = interests
                                    settings[OnboardingCompletedKey] = true
                                }
                            }
                            finish(interests)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    uiLanguage: String,
    onUiLanguageChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(
                painter = painterResource(R.drawable.jix_music),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            listOf(
                "system" to stringResource(R.string.lang_system),
                "en" to "English",
                "fa" to "فارسی",
            ).forEach { (key, label) ->
                val isSelected = uiLanguage == key
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .clickable { onUiLanguageChange(key) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onContinue() },
        ) {
            Text(
                text = stringResource(R.string.onboarding_continue),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterestsStep(
    selected: List<String>,
    onToggle: (String) -> Unit,
    onDone: (String) -> Unit,
) {
    val context = LocalContext.current

    val moods by produceState(initialValue = emptyList<MoodAndGenres.Item>()) {
        YouTube.moodAndGenres().onSuccess { sections ->
            value = sections.flatMap { it.items }.distinctBy { it.title }
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_interests_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_interests_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        if (moods.isEmpty()) {
            LaunchedEffect(Unit) {} // no-op keeps composition stable
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 48.dp),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_loading_moods),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                moods.forEach { mood ->
                    val key = "${mood.endpoint.browseId}$FIELD${mood.endpoint.params ?: ""}$FIELD${mood.title}"
                    val isSelected = key in selected
                    val stripe = if (mood.stripeColor != 0L) {
                        Color(mood.stripeColor)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) stripe else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = stripe.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(50),
                            )
                            .clickable { onToggle(key) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(stripe),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = mood.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = if (isSelected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        val enough = selected.size >= 3
        Text(
            text = stringResource(R.string.onboarding_selected_count, selected.size),
            style = MaterialTheme.typography.labelMedium,
            color = if (enough) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (enough) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                )
                .clickable(enabled = enough) {
                    onDone(selected.joinToString(ENTRY.toString()))
                },
        ) {
            Text(
                text = stringResource(R.string.onboarding_finish),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (enough) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
