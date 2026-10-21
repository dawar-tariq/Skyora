package com.skyora.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.MoveDirection
import com.skyora.weather.domain.model.SavedCity
import com.skyora.weather.domain.repository.SavedCitiesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

class SavedCitiesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : SavedCitiesRepository {

    private val savedCitiesKey = stringPreferencesKey("saved_cities")

    override val savedCities: Flow<List<SavedCity>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { decode(it[savedCitiesKey]) }
        .distinctUntilChanged()

    override suspend fun add(location: Location, nowMillis: Long): Boolean {
        var added = false
        dataStore.edit { prefs ->
            val current = decode(prefs[savedCitiesKey])
            if (current.any { it.location.isSamePlaceAs(location) }) return@edit
            prefs[savedCitiesKey] = json.encodeToString(current + SavedCity(location, nowMillis))
            added = true
        }
        return added
    }

    override suspend fun remove(locationId: String): SavedCity? {
        var removed: SavedCity? = null
        dataStore.edit { prefs ->
            val current = decode(prefs[savedCitiesKey])
            val index = current.indexOfFirst { it.location.id == locationId }
            if (index == -1) return@edit
            removed = current[index]
            prefs[savedCitiesKey] = json.encodeToString(current.filterIndexed { i, _ -> i != index })
        }
        return removed
    }

    override suspend fun restore(savedCity: SavedCity, index: Int) {
        dataStore.edit { prefs ->
            val current = decode(prefs[savedCitiesKey]).toMutableList()
            if (current.any { it.location.isSamePlaceAs(savedCity.location) }) return@edit
            val target = index.coerceIn(0, current.size)
            current.add(target, savedCity)
            prefs[savedCitiesKey] = json.encodeToString(current)
        }
    }

    override suspend fun move(locationId: String, direction: MoveDirection): Boolean {
        var moved = false
        dataStore.edit { prefs ->
            val current = decode(prefs[savedCitiesKey]).toMutableList()
            val index = current.indexOfFirst { it.location.id == locationId }
            if (index == -1) return@edit
            val target = if (direction == MoveDirection.UP) index - 1 else index + 1
            if (target !in current.indices) return@edit
            val item = current.removeAt(index)
            current.add(target, item)
            prefs[savedCitiesKey] = json.encodeToString(current)
            moved = true
        }
        return moved
    }

    private fun decode(raw: String?): List<SavedCity> =
        raw?.let { runCatching { json.decodeFromString<List<SavedCity>>(it) }.getOrNull() }.orEmpty()
}
