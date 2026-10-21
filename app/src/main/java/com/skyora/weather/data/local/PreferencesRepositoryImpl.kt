package com.skyora.weather.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyora.weather.domain.model.DefaultLocations
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.NotificationSettings
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

    // Notifications
    val dailyForecastEnabled = booleanPreferencesKey("notif_daily_enabled")
    val dailyForecastMinutes = intPreferencesKey("notif_daily_minutes")
    val rainAlertsEnabled = booleanPreferencesKey("notif_rain_enabled")
    val rainThresholdPercent = intPreferencesKey("notif_rain_threshold")
    val notificationLocation = stringPreferencesKey("notif_location")
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

    override suspend fun setDailyForecastEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.dailyForecastEnabled] = enabled
            if (enabled && prefs[PreferenceKeys.dailyForecastMinutes] == null) {
                prefs[PreferenceKeys.dailyForecastMinutes] = DEFAULT_DAILY_MINUTES
            }
        }
    }

    override suspend fun setDailyForecastMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.dailyForecastMinutes] = minutes.coerceIn(0, MINUTES_PER_DAY - 1)
        }
    }

    override suspend fun setRainAlertsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[PreferenceKeys.rainAlertsEnabled] = enabled }
    }

    override suspend fun setRainThresholdPercent(percent: Int) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.rainThresholdPercent] = percent.coerceIn(20, 95)
        }
    }

    override suspend fun setNotificationLocation(location: Location?) {
        dataStore.edit { prefs ->
            if (location == null) {
                prefs.remove(PreferenceKeys.notificationLocation)
            } else {
                prefs[PreferenceKeys.notificationLocation] = json.encodeToString(location)
            }
        }
    }

    override suspend fun clearNotificationSettings() {
        dataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.dailyForecastEnabled)
            prefs.remove(PreferenceKeys.dailyForecastMinutes)
            prefs.remove(PreferenceKeys.rainAlertsEnabled)
            prefs.remove(PreferenceKeys.rainThresholdPercent)
            prefs.remove(PreferenceKeys.notificationLocation)
        }
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
            notifications = NotificationSettings(
                dailyForecastEnabled = this[PreferenceKeys.dailyForecastEnabled] ?: false,
                dailyForecastMinutes = (this[PreferenceKeys.dailyForecastMinutes]
                    ?: DEFAULT_DAILY_MINUTES).coerceIn(0, MINUTES_PER_DAY - 1),
                rainAlertsEnabled = this[PreferenceKeys.rainAlertsEnabled] ?: false,
                rainThresholdPercent = (this[PreferenceKeys.rainThresholdPercent] ?: 60)
                    .coerceIn(20, 95),
                notificationLocation = decodeLocation(this[PreferenceKeys.notificationLocation]),
            ),
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
        const val DEFAULT_DAILY_MINUTES = 7 * 60
        const val MINUTES_PER_DAY = 24 * 60
    }
}
