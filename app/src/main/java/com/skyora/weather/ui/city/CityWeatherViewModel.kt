package com.skyora.weather.ui.city

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.R
import com.skyora.weather.core.network.NetworkMonitor
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.SavedCity
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.domain.model.WeatherResult
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.domain.repository.PreferencesRepository
import com.skyora.weather.domain.repository.SavedCitiesRepository
import com.skyora.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

data class CityWeatherUiState(
    val location: Location,
    val snapshot: WeatherSnapshot? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: WeatherError? = null,
    val isSaved: Boolean = false,
    val isHomeCity: Boolean = false,
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val isOnline: Boolean = true,
) {
    val showingCachedData: Boolean get() = snapshot != null && error != null
}

sealed interface CityEvent {
    data class Snackbar(@StringRes val messageRes: Int, val args: List<String> = emptyList()) : CityEvent
}

class CityWeatherViewModel(
    private val location: Location,
    private val weatherRepository: WeatherRepository,
    private val preferencesRepository: PreferencesRepository,
    private val savedCitiesRepository: SavedCitiesRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(CityWeatherUiState(location = location))
    val state: StateFlow<CityWeatherUiState> = _state.asStateFlow()

    private val _events = Channel<CityEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var refreshJob: Job? = null
    private var savedCities: List<SavedCity> = emptyList()

    init {
        observePreferences()
        observeSavedCities()
        observeCachedWeather()
        observeConnectivity()
        loadWeather(forceRefresh = false)
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences
                .distinctUntilChanged()
                .collect { prefs ->
                    _state.update {
                        it.copy(
                            unit = prefs.temperatureUnit,
                            isHomeCity = prefs.selectedLocation.isSamePlaceAs(location),
                            isSaved = savedCities.any { city -> city.location.isSamePlaceAs(location) },
                        )
                    }
                }
        }
    }

    private fun observeSavedCities() {
        viewModelScope.launch {
            savedCitiesRepository.savedCities.collect { cities ->
                savedCities = cities
                _state.update { it.copy(isSaved = cities.any { city -> city.location.isSamePlaceAs(location) }) }
            }
        }
    }

    private fun observeCachedWeather() {
        viewModelScope.launch {
            weatherRepository.observeWeather(location.id).collect { snapshot ->
                if (snapshot != null) {
                    _state.update { it.copy(snapshot = snapshot, isLoading = false) }
                }
            }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _state.update { it.copy(isOnline = online) }
            }
        }
    }

    private fun loadWeather(forceRefresh: Boolean) {
        if (refreshJob?.isActive == true && !forceRefresh) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hasData = _state.value.snapshot != null
            _state.update { it.copy(isRefreshing = true, isLoading = !hasData, error = null) }
            when (val result = weatherRepository.loadWeather(location, forceRefresh)) {
                is WeatherResult.Success -> _state.update {
                    it.copy(
                        snapshot = result.snapshot,
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                    )
                }

                is WeatherResult.Failure -> _state.update { current ->
                    current.copy(
                        snapshot = result.cachedSnapshot ?: current.snapshot,
                        isLoading = false,
                        isRefreshing = false,
                        error = result.error,
                    )
                }
            }
        }
    }

    fun refresh() = loadWeather(forceRefresh = true)

    fun toggleSaved(displayName: String) {
        viewModelScope.launch {
            if (_state.value.isSaved) {
                val saved = savedCitiesRepository.savedCities.first()
                    .firstOrNull { it.location.isSamePlaceAs(location) }
                if (saved != null) {
                    savedCitiesRepository.remove(saved.location.id)
                    _events.send(CityEvent.Snackbar(R.string.city_removed_snackbar, listOf(displayName)))
                }
            } else {
                val added = savedCitiesRepository.add(location, clock.millis())
                if (added) {
                    _events.send(CityEvent.Snackbar(R.string.city_added_snackbar, listOf(displayName)))
                } else {
                    _events.send(CityEvent.Snackbar(R.string.snackbar_saved_duplicate))
                }
            }
        }
    }

    fun setAsHome(displayName: String) {
        viewModelScope.launch {
            preferencesRepository.setHomeCity(location)
            _events.send(CityEvent.Snackbar(R.string.city_home_snackbar, listOf(displayName)))
        }
    }

    companion object {
        fun factory(container: AppContainer, location: Location): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    CityWeatherViewModel(
                        location = location,
                        weatherRepository = container.weatherRepository,
                        preferencesRepository = container.preferencesRepository,
                        savedCitiesRepository = container.savedCitiesRepository,
                        networkMonitor = container.networkMonitor,
                        clock = container.clock,
                    )
                }
            }
    }
}
