package com.skyora.weather.domain.repository

import com.skyora.weather.domain.model.DeviceLocationResult
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.MoveDirection
import com.skyora.weather.domain.model.SavedCity
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.model.UserPreferences
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.domain.model.WeatherResult
import com.skyora.weather.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    /** Emits updates from the in-memory cache for a given location id. */
    fun observeWeather(locationId: String): Flow<WeatherSnapshot?>

    suspend fun cachedWeather(locationId: String): WeatherSnapshot?

    /**
     * Loads a forecast, using the local cache when it is fresh. Pass
     * `forceRefresh = true` for an explicit user refresh.
     */
    suspend fun loadWeather(location: Location, forceRefresh: Boolean = false): WeatherResult

    suspend fun clearCache()
}

sealed interface CitySearchResult {
    data class Success(val locations: List<Location>) : CitySearchResult
    data class Failure(val error: WeatherError) : CitySearchResult
}

interface GeocodingRepository {
    suspend fun searchCities(query: String): CitySearchResult
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setTemperatureUnit(unit: TemperatureUnit)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setAutomaticLocationEnabled(enabled: Boolean)

    suspend fun setSelectedLocation(location: Location)

    suspend fun setDefaultCity(location: Location)

    /**
     * Makes [location] the Home city and turns automatic location off, so the
     * explicit choice is not immediately overwritten by a GPS fix.
     */
    suspend fun setHomeCity(location: Location)

    suspend fun addRecentSearch(location: Location)

    suspend fun clearRecentSearches()
}

interface SavedCitiesRepository {
    val savedCities: Flow<List<SavedCity>>

    /** Returns true when the city was added, false when it was already saved. */
    suspend fun add(location: Location, nowMillis: Long): Boolean

    /** Returns the removed entry so the UI can offer undo. */
    suspend fun remove(locationId: String): SavedCity?

    suspend fun restore(savedCity: SavedCity, index: Int)

    suspend fun move(locationId: String, direction: MoveDirection): Boolean
}

interface DeviceLocationRepository {
    suspend fun getCurrentLocation(hasFinePermission: Boolean): DeviceLocationResult
}
