package com.skyora.weather.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.R
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.location.LocationCoordinator
import com.skyora.weather.domain.model.DefaultLocations
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.LocationAccessState
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.repository.PreferencesRepository
import com.skyora.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val automaticLocationEnabled: Boolean = true,
    val homeCity: Location = DefaultLocations.fallback,
    val selectedLocation: Location = DefaultLocations.fallback,
    val isLocating: Boolean = false,
    val versionName: String = "",
)

sealed interface SettingsEvent {
    data class Snackbar(@StringRes val messageRes: Int) : SettingsEvent

    /** Emitted once onboarding=false has been persisted. */
    data object OnboardingReset : SettingsEvent
}

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val weatherRepository: WeatherRepository,
    private val locationCoordinator: LocationCoordinator,
    private val versionName: String,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        locationCoordinator.state,
    ) { prefs, access ->
        SettingsUiState(
            unit = prefs.temperatureUnit,
            automaticLocationEnabled = prefs.automaticLocationEnabled,
            homeCity = prefs.defaultCity,
            selectedLocation = prefs.selectedLocation,
            isLocating = access is LocationAccessState.Locating,
            versionName = versionName,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(versionName = versionName),
    )

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastLocationState: LocationAccessState? = null

    init {
        viewModelScope.launch {
            locationCoordinator.state.collect { state ->
                val previous = lastLocationState
                if (previous != null && state is LocationAccessState.Error &&
                    previous !is LocationAccessState.Error
                ) {
                    _events.send(SettingsEvent.Snackbar(R.string.settings_location_unavailable_snackbar))
                }
                lastLocationState = state
            }
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { preferencesRepository.setTemperatureUnit(unit) }
    }

    fun enableAutomaticLocation(hasFinePermission: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutomaticLocationEnabled(true)
            locationCoordinator.locate(hasFinePermission, !hasFinePermission)
        }
    }

    fun disableAutomaticLocation() {
        viewModelScope.launch {
            preferencesRepository.setAutomaticLocationEnabled(false)
            locationCoordinator.reset()
        }
    }

    fun onLocationPermissionDenied() {
        viewModelScope.launch {
            _events.send(SettingsEvent.Snackbar(R.string.settings_location_denied_snackbar))
        }
    }

    fun refreshCurrentLocation(hasFinePermission: Boolean) {
        viewModelScope.launch {
            locationCoordinator.locate(hasFinePermission, !hasFinePermission)
        }
    }

    fun clearCachedWeather() {
        viewModelScope.launch {
            weatherRepository.clearCache()
            _events.send(SettingsEvent.Snackbar(R.string.settings_cache_cleared))
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferencesRepository.clearRecentSearches()
            _events.send(SettingsEvent.Snackbar(R.string.settings_recents_cleared))
        }
    }

    private var resetRequested = false

    fun resetOnboarding() {
        if (resetRequested) return
        resetRequested = true
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
            _events.send(SettingsEvent.OnboardingReset)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    preferencesRepository = container.preferencesRepository,
                    weatherRepository = container.weatherRepository,
                    locationCoordinator = container.locationCoordinator,
                    versionName = container.appVersionName,
                )
            }
        }
    }
}
