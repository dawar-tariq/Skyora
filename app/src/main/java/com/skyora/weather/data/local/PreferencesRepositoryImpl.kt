package com.skyora.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyora.weather.domain.model.DefaultLocations
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.model.UserPreferences
import com.skyora.weather.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private object PreferenceKeys {
    val temperatureUnit = stringPreferencesKey("temperature_unit")
    val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
    val automaticLocation = booleanPreferencesKey("automatic_location")
    val selectedLocation = stringPreferencesKey("selected_location")
    val defaultCity = stringPreferencesKey("default_city")
    val recentSearches = stringPreferencesKey("recent_searches")
}

class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { it.toUserPreferences() }
        .distinctUntilChanged()

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        dataStore.edit { it[PreferenceKeys.temperatureUnit] = unit.name }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[PreferenceKeys.onboardingCompleted] = completed }
    }

    override suspend fun setAutomaticLocationEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.automaticLocation] = enabled }
    }

    override suspend fun setSelectedLocation(location: Location) {
        dataStore.edit { it[PreferenceKeys.selectedLocation] = json.encodeToString(location) }
    }

    override suspend fun setDefaultCity(location: Location) {
        dataStore.edit { it[PreferenceKeys.defaultCity] = json.encodeToString(location) }
    }

    override suspend fun setHomeCity(location: Location) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.selectedLocation] = json.encodeToString(location)
            prefs[PreferenceKeys.defaultCity] = json.encodeToString(location)
            prefs[PreferenceKeys.automaticLocation] = false
        }
    }

    override suspend fun addRecentSearch(location: Location) {
        dataStore.edit { prefs ->
            val current = decodeLocations(prefs[PreferenceKeys.recentSearches])
            val updated = (listOf(location) + current.filterNot { it.isSamePlaceAs(location) })
                .take(MAX_RECENT_SEARCHES)
            prefs[PreferenceKeys.recentSearches] = json.encodeToString(updated)
        }
    }

    override suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(PreferenceKeys.recentSearches) }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val unit = this[PreferenceKeys.temperatureUnit]
            ?.let { name -> runCatching { TemperatureUnit.valueOf(name) }.getOrNull() }
            ?: TemperatureUnit.CELSIUS
        val selected = decodeLocation(this[PreferenceKeys.selectedLocation])
            ?: DefaultLocations.fallback
        val default = decodeLocation(this[PreferenceKeys.defaultCity])
            ?: DefaultLocations.fallback
        return UserPreferences(
            temperatureUnit = unit,
            onboardingCompleted = this[PreferenceKeys.onboardingCompleted] ?: false,
            automaticLocationEnabled = this[PreferenceKeys.automaticLocation] ?: true,
            selectedLocation = selected,
            defaultCity = default,
            recentSearches = decodeLocations(this[PreferenceKeys.recentSearches]),
        )
    }

    private fun decodeLocation(raw: String?): Location? =
        raw?.let { runCatching { json.decodeFromString<Location>(it) }.getOrNull() }

    private fun decodeLocations(raw: String?): List<Location> =
        raw?.let {
            runCatching { json.decodeFromString<List<Location>>(it) }.getOrNull()
        }.orEmpty()

    private companion object {
        const val MAX_RECENT_SEARCHES = 6
    }
}
