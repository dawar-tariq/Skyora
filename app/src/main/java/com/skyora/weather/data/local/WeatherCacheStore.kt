package com.skyora.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyora.weather.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Small on-disk cache of the most recent successful responses.
 *
 * The parsed snapshot is stored, so restoring the app after being offline does
 * not need a network round-trip. Each entry keeps its original fetch time which
 * the UI shows instead of pretending the data is live.
 */
class WeatherCacheStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    suspend fun readAll(): List<WeatherSnapshot> {
        val values = dataStore.data.first()
        return values.asMap()
            .filterKeys { it.name.startsWith(KEY_PREFIX) }
            .values
            .mapNotNull { (it as? String)?.let(::decode) }
    }

    suspend fun write(snapshot: WeatherSnapshot) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(KEY_PREFIX + snapshot.location.id)] =
                json.encodeToString(snapshot)
            prune(prefs)
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            val keys = prefs.asMap().keys
                .filter { it.name.startsWith(KEY_PREFIX) }
                .map { stringPreferencesKey(it.name) }
            keys.forEach { prefs.remove(it) }
        }
    }

    private fun prune(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        val entries = prefs.asMap()
            .filterKeys { it.name.startsWith(KEY_PREFIX) }
            .mapNotNull { (key, value) ->
                val snapshot = (value as? String)?.let(::decode) ?: return@mapNotNull null
                key.name to snapshot.fetchedAtEpochMillis
            }
        if (entries.size <= MAX_ENTRIES) return
        entries.sortedBy { it.second }
            .take(entries.size - MAX_ENTRIES)
            .forEach { (name, _) -> prefs.remove(stringPreferencesKey(name)) }
    }

    private fun decode(raw: String): WeatherSnapshot? =
        runCatching { json.decodeFromString<WeatherSnapshot>(raw) }.getOrNull()

    private companion object {
        const val KEY_PREFIX = "weather:"
        const val MAX_ENTRIES = 10
    }
}
