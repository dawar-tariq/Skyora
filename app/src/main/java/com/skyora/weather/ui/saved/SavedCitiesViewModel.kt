package com.skyora.weather.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.core.network.NetworkMonitor
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.MoveDirection
import com.skyora.weather.domain.model.SavedCity
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.domain.model.WeatherResult
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.domain.repository.PreferencesRepository
import com.skyora.weather.domain.repository.SavedCitiesRepository
import com.skyora.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class SavedCityRow(
    val saved: SavedCity,
    val snapshot: WeatherSnapshot? = null,
    val error: WeatherError? = null,
) {
    val isLoading: Boolean get() = snapshot == null && error == null
}

data class SavedCitiesUiState(
    val rows: List<SavedCityRow> = emptyList(),
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
) {
    val hasPartialFailures: Boolean
        get() = rows.any { it.snapshot == null && it.error != null }
}

sealed interface SavedEvent {
    data class Removed(val displayName: String) : SavedEvent
    data object Restored : SavedEvent
}

class SavedCitiesViewModel(
    private val savedCitiesRepository: SavedCitiesRepository,
    private val weatherRepository: WeatherRepository,
    private val preferencesRepository: PreferencesRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(SavedCitiesUiState())
    val state: StateFlow<SavedCitiesUiState> = _state.asStateFlow()

    private val _events = Channel<SavedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastRemoved: Pair<SavedCity, Int>? = null
    private var manualRefreshRunning = false

    init {
        observeUnit()
        observeConnectivity()
        observeSavedCities()
    }

    private fun observeUnit() {
        viewModelScope.launch {
            preferencesRepository.preferences
                .map { it.temperatureUnit }
                .distinctUntilChanged()
                .collect { unit -> _state.update { it.copy(unit = unit) } }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> _state.update { it.copy(isOnline = online) } }
        }
    }

    private fun observeSavedCities() {
        viewModelScope.launch {
            savedCitiesRepository.savedCities.collectLatest { cities ->
                _state.update { current ->
                    current.copy(
                        rows = mergeRows(cities, current.rows),
                        isLoading = false,
                    )
                }
                if (cities.isEmpty()) {
                    _state.update { it.copy(isRefreshing = false) }
                    return@collectLatest
                }
                coroutineScope {
                    launch { refreshWeather(cities, force = false) }
                    cities.forEach { city ->
                        launch {
                            weatherRepository.observeWeather(city.location.id).collect { snapshot ->
                                if (snapshot != null) {
                                    updateRow(city.location.id) {
                                        it.copy(snapshot = snapshot, error = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun mergeRows(cities: List<SavedCity>, existing: List<SavedCityRow>): List<SavedCityRow> =
        cities.map { city ->
            existing.firstOrNull { it.saved.location.id == city.location.id }
                ?: SavedCityRow(saved = city)
        }

    private fun updateRow(locationId: String, transform: (SavedCityRow) -> SavedCityRow) {
        _state.update { current ->
            current.copy(
                rows = current.rows.map { row ->
                    if (row.saved.location.id == locationId) transform(row) else row
                },
            )
        }
    }

    private suspend fun refreshWeather(cities: List<SavedCity>, force: Boolean) {
        _state.update { it.copy(isRefreshing = true) }
        try {
            val semaphore = Semaphore(4)
            coroutineScope {
                cities.forEach { city ->
                    launch {
                        semaphore.withPermit {
                            when (val result = weatherRepository.loadWeather(city.location, force)) {
                                is WeatherResult.Success -> updateRow(city.location.id) {
                                    it.copy(snapshot = result.snapshot, error = null)
                                }

                                is WeatherResult.Failure -> updateRow(city.location.id) {
                                    it.copy(
                                        snapshot = result.cachedSnapshot ?: it.snapshot,
                                        error = result.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            _state.update { it.copy(isRefreshing = false, isLoading = false) }
        }
    }

    fun refresh() {
        if (manualRefreshRunning) return
        val cities = _state.value.rows.map { it.saved }
        if (cities.isEmpty()) return
        manualRefreshRunning = true
        viewModelScope.launch {
            try {
                refreshWeather(cities, force = true)
            } finally {
                manualRefreshRunning = false
            }
        }
    }

    fun remove(saved: SavedCity, displayName: String) {
        viewModelScope.launch {
            val index = _state.value.rows.indexOfFirst { it.saved.location.id == saved.location.id }
            val removed = savedCitiesRepository.remove(saved.location.id)
            if (removed != null) {
                lastRemoved = removed to index.coerceAtLeast(0)
                _events.send(SavedEvent.Removed(displayName))
            }
        }
    }

    fun undoRemove() {
        val pending = lastRemoved ?: return
        lastRemoved = null
        viewModelScope.launch {
            savedCitiesRepository.restore(pending.first, pending.second)
            _events.send(SavedEvent.Restored)
        }
    }

    fun move(locationId: String, direction: MoveDirection) {
        viewModelScope.launch { savedCitiesRepository.move(locationId, direction) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SavedCitiesViewModel(
                    savedCitiesRepository = container.savedCitiesRepository,
                    weatherRepository = container.weatherRepository,
                    preferencesRepository = container.preferencesRepository,
                    networkMonitor = container.networkMonitor,
                )
            }
        }
    }
}
