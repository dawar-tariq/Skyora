package com.skyora.weather.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile

/**
 * Independent DataStore files keep user preferences safe when the weather cache
 * is cleared, keep cache writes away from the preferences file, and let the
 * widget-refresh journal survive "clear cache" in Settings.
 */
object SkyoraDataStores {
    fun preferences(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("skyora_preferences") },
        )

    fun weatherCache(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("skyora_weather_cache") },
        )

    /** Background-refresh journal, read by the Widget reliability screen. */
    fun widgetStatus(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("skyora_widget_status") },
        )
}
