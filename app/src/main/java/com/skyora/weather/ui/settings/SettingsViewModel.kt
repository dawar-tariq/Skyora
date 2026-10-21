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
import com.skyora.weather.domain.model.NotificationSettings
import com.skyora.weather.domain.model.SavedCity
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.repository.PreferencesRepository
import com.skyora.weather.domain.repository.SavedCitiesRepository
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
    val notifications: NotificationSettings = NotificationSettings(),
    val savedCities: List<SavedCity> = emptyList(),
) {
    /** Master switch state: any notification type enabled. */
    val notificationsEnabled: Boolean get() = notifications.anyEnabled
}

sealed interface SettingsEvent {
    data class Snackbar(@StringRes val messageRes: Int) : SettingsEvent

    /** Snackbar with format arguments, e.g. "Notifications will use %1$s". */
    data class SnackbarText(
        @StringRes val messageRes: Int,
        val args: List<String> = emptyList(),
    ) : SettingsEvent

    /** Emitted once onboarding=false has been persisted. */
    data object OnboardingReset : SettingsEvent
}

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val weatherRepository: WeatherRepository,
    private val locationCoordinator: LocationCoordinator,
    private val savedCitiesRepository: SavedCitiesRepository,
    private val versionName: String,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        locationCoordinator.state,
        savedCitiesRepository.savedCities,
    ) { prefs, access, saved ->
        SettingsUiState(
            unit = prefs.temperatureUnit,
            automaticLocationEnabled = prefs.automaticLocationEnabled,
            homeCity = prefs.defaultCity,
            selectedLocation = prefs.selectedLocation,
            isLocating = access is LocationAccessState.Locating,
            versionName = versionName,
            notifications = prefs.notifications,
            savedCities = saved,
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

    // --- notifications -------------------------------------------------------

    fun enableNotifications() {
        viewModelScope.launch {
            // Master switch turns on the daily summary; rain alerts stay opt-in.
            preferencesRepository.setDailyForecastEnabled(true)
            _events.send(SettingsEvent.Snackbar(R.string.settings_notif_enabled_snackbar))
        }
    }

    fun disableNotifications() {
        viewModelScope.launch {
            preferencesRepository.setDailyForecastEnabled(false)
            preferencesRepository.setRainAlertsEnabled(false)
            _events.send(SettingsEvent.Snackbar(R.string.settings_notif_disabled_snackbar))
        }
    }

    fun setDailyForecastEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDailyForecastEnabled(enabled)
            if (!enabled && !currentState().notifications.rainAlertsEnabled) {
                _events.send(SettingsEvent.Snackbar(R.string.settings_notif_disabled_snackbar))
            }
        }
    }

    fun setDailyForecastTime(minutesAfterMidnight: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyForecastMinutes(minutesAfterMidnight)
            val label = "%02d:%02d".format(minutesAfterMidnight / 60, minutesAfterMidnight % 60)
            _events.send(SettingsEvent.SnackbarText(R.string.settings_notif_time_set, listOf(label)))
        }
    }

    fun setRainAlertsEnabled(enabled: Boolean) {
        // Independent from the daily summary: either feature alone keeps the
        // master switch on, because the master state is "any alert enabled".
        viewModelScope.launch { preferencesRepository.setRainAlertsEnabled(enabled) }
    }

    fun setRainThreshold(percent: Int) {
        viewModelScope.launch { preferencesRepository.setRainThresholdPercent(percent) }
    }

    fun setNotificationLocation(location: Location) {
        viewModelScope.launch {
            preferencesRepository.setNotificationLocation(location)
            _events.send(
                SettingsEvent.SnackbarText(
                    R.string.settings_notif_city_set,
                    listOf(location.name),
                ),
            )
        }
    }

    fun onNotificationPermissionDenied() {
        viewModelScope.launch {
            _events.send(SettingsEvent.Snackbar(R.string.settings_notif_permission_denied))
        }
    }

    private fun currentState(): SettingsUiState = state.value

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
                    savedCitiesRepository = container.savedCitiesRepository,
                    versionName = container.appVersionName,
                )
            }
        }
    }
}
