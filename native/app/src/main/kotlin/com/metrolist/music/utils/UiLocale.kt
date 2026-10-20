package com.metrolist.music.utils

import android.content.Context
import android.content.res.Configuration
import com.metrolist.music.constants.UiLanguageKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * JixOne 5.0 in-app language switch (fa/en) without appcompat:
 * MainActivity.attachBaseContext wraps the base context with the stored
 * locale; changing the setting writes the pref and recreates the activity.
 */
fun Context.applyUiLanguage(): Context {
    val lang = try {
        runBlocking {
            withTimeoutOrNull(400) {
                dataStore.data.first()[UiLanguageKey]
            }
        }
    } catch (e: Exception) {
        null
    } ?: return this

    if (lang != "fa" && lang != "en") return this

    return try {
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        createConfigurationContext(config)
    } catch (e: Exception) {
        this
    }
}
