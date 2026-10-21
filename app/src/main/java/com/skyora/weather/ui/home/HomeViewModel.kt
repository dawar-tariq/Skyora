package com.skyora.weather.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.R
import com.skyora.weather.core.network.NetworkMonitor
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.location.LocationCoordinator
import com.skyora.weather.domain.model.DefaultLocations
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.LocationAccessState
import com.skyora.weather.domain.model.SavedCity
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.domain.model.WeatherResult
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.domain.repository.PreferencesRepository
import com.skyora.weather.domain.repository.SavedCitiesRepository
import com.skyora.weather.domain.repository.WeatherRepository
import com.skyora.weather.domain.widget.WidgetNudgeRules
import com.skyora.weather.domain.widget.WidgetNudgeState
import com.skyora.weather.domain.widget.WidgetRefreshStatus
import com.skyora.weather.widget.WidgetRefreshStatusStore
import com.skyora.weather.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Clock

data class HomeUiState(
    val location: Location = DefaultLocations.fallback,
    val defaultCity: Location = DefaultLocations.fallback,
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val automaticLocationEnabled: Boolean = true,
    val snapshot: WeatherSnapshot? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: WeatherError? = null,
    val isOnline: Boolean = true,
    val isSaved: Boolean = false,
    val locationAccess: LocationAccessState = LocationAccessState.Idle,
    /** Widget health, for the Settings gear dot. */
    val widgetNeedsAttention: Boolean = false,
    /** Whether the "your widget isn't updating" banner should be on screen. */
    val showWidgetBanner: Boolean = false,
    /** How old the widget's data is, for the banner copy. */
    val widgetDataAgeMillis: Long = 0L,
) {
    val isLocating: Boolean get() = locationAccess is LocationAccessState.Locating

    val approximateLocation: Boolean
        get() = location.isDeviceLocation &&
            (locationAccess as? LocationAccessState.Success)?.isApproximate == true

    val showingCachedData: Boolean get() = snapshot != null && error != null
}

enum class SnackbarAction { OPEN_APP_SETTINGS }

sealed interface HomeEvent {
    data class Snackbar(
        @StringRes val messageRes: Int,
        val args: List<String> = emptyList(),
        @StringRes val actionLabelRes: Int? = null,
        val action: SnackbarAction? = null,
    ) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val savedCitiesRepository: SavedCitiesRepository,
    private val weatherRepository: WeatherRepository,
    private val locationCoordinator: LocationCoordinator,
    private val networkMonitor: NetworkMonitor,
    private val widgetStatusStore: WidgetRefreshStatusStore,
    /** Whether a widget is placed; a lambda keeps Android types out of the VM. */
    private val hasWidgets: () -> Boolean,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var refreshJob: Job? = null
    private var savedCityList: List<SavedCity> = emptyList()
    private var firstPreferencesEmission = true
    private var lastAutoLocateAtMillis = 0L

    init {
        observePreferences()
        observeCachedWeather()
        observeSavedCities()
        observeConnectivity()
        observeLocationAccess()
        observeWidgetAttention()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                val locationChanged = _state.value.location.id != prefs.selectedLocation.id
                val isFirst = firstPreferencesEmission
                firstPreferencesEmission = false
                _state.update { current ->
                    current.copy(
                        location = prefs.selectedLocation,
                        defaultCity = prefs.defaultCity,
                        unit = prefs.temperatureUnit,
                        automaticLocationEnabled = prefs.automaticLocationEnabled,
                        snapshot = if (locationChanged) null else current.snapshot,
                        error = if (locationChanged) null else current.error,
                        isLoading = if (locationChanged) true else current.isLoading,
                        isSaved = savedCityList.any { it.location.isSamePlaceAs(prefs.selectedLocation) },
                    )
                }
                if (isFirst || locationChanged) {
                    loadWeather(prefs.selectedLocation, forceRefresh = false)
                }
            }
        }
    }

    private fun observeCachedWeather() {
        viewModelScope.launch {
            _state
                .map { it.location.id }
                .distinctUntilChanged()
                .flatMapLatest { locationId -> weatherRepository.observeWeather(locationId) }
                .collect { snapshot ->
                    if (snapshot != null) {
                        _state.update { current ->
                            if (current.location.id == snapshot.location.id) {
                                current.copy(snapshot = snapshot, isLoading = false)
                            } else {
                                current
                            }
                        }
                    }
                }
        }
    }

    private fun observeSavedCities() {
        viewModelScope.launch {
            savedCitiesRepository.savedCities.collect { cities ->
                savedCityList = cities
                val currentLocation = _state.value.location
                _state.update {
                    it.copy(isSaved = cities.any { city -> city.location.isSamePlaceAs(currentLocation) })
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

    private fun observeLocationAccess() {
        viewModelScope.launch {
            locationCoordinator.state.collect { access ->
                _state.update { it.copy(locationAccess = access) }
                when (access) {
                    is LocationAccessState.Locating, is LocationAccessState.Success -> {
                        lastAutoLocateAtMillis = clock.millis()
                    }

                    is LocationAccessState.PermissionRequired -> Unit

                    is LocationAccessState.Error -> {
                        _events.send(HomeEvent.Snackbar(R.string.snackbar_location_unavailable))
                    }

                    LocationAccessState.Idle -> Unit
                }
            }
        }
    }

    /**
     * Decides whether to mention that the widget stopped refreshing.
     *
     * The ticker matters: without it a user who leaves Home open would keep
     * seeing a healthy app after the background job died. It is a 60-second
     * timer over an in-memory journal, not a network call.
     */
    private fun observeWidgetAttention() {
        viewModelScope.launch {
            combine(
                widgetStatusStore.status,
                widgetStatusStore.nudge,
                ticker(),
                networkMonitor.isOnline,
            ) { status, nudge, now, online -> WidgetSignal(status, nudge, now, online) }
                .collect { (status, nudge, now, online) ->
                    val widgets = withContext(Dispatchers.Default) {
                        runCatching { hasWidgets() }.getOrDefault(false)
                    }
                    val interval = WidgetUpdater.PERIOD_MINUTES
                    val attention = WidgetNudgeRules.needsAttention(
                        status = status,
                        nowMillis = now,
                        requestedIntervalMinutes = interval,
                        hasWidgets = widgets,
                        isOnline = online,
                    )
                    val showBanner = WidgetNudgeRules.shouldShowBanner(
                        status = status,
                        nudge = nudge,
                        nowMillis = now,
                        requestedIntervalMinutes = interval,
                        hasWidgets = widgets,
                        isOnline = online,
                    )
                    _state.update { current ->
                        current.copy(
                            widgetNeedsAttention = attention,
                            showWidgetBanner = showBanner,
                            widgetDataAgeMillis = if (status.lastSuccessMillis > 0L) {
                                (now - status.lastSuccessMillis).coerceAtLeast(0L)
                            } else {
                                0L
                            },
                        )
                    }
                }
        }
    }

    /** One evaluation of "should we mention the widget?" with all its inputs. */
    private data class WidgetSignal(
        val status: WidgetRefreshStatus,
        val nudge: WidgetNudgeState,
        val nowMillis: Long,
        val isOnline: Boolean,
    )

    private fun ticker(): Flow<Long> = flow {
        while (true) {
            emit(clock.millis())
            delay(60_000L)
        }
    }

    /** Hides the banner for this episode (see [WidgetNudgeRules]). */
    fun dismissWidgetBanner() {
        _state.update { it.copy(showWidgetBanner = false) }
        viewModelScope.launch {
            widgetStatusStore.dismissNudge(clock.millis())
        }
    }

    private fun loadWeather(location: Location, forceRefresh: Boolean) {
        if (refreshJob?.isActive == true && !forceRefresh) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hasDataForLocation = _state.value.snapshot != null && _state.value.location.id == location.id
            _state.update {
                it.copy(
                    isRefreshing = true,
                    isLoading = !hasDataForLocation,
                    error = null,
                )
            }
            when (val result = weatherRepository.loadWeather(location, forceRefresh)) {
                is WeatherResult.Success -> {
                    _state.update {
                        it.copy(
                            snapshot = result.snapshot,
                            isRefreshing = false,
                            isLoading = false,
                            error = null,
                        )
                    }
                }

                is WeatherResult.Failure -> {
                    _state.update { current ->
                        current.copy(
                            snapshot = result.cachedSnapshot ?: current.snapshot,
                            isRefreshing = false,
                            isLoading = false,
                            error = result.error,
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        loadWeather(_state.value.location, forceRefresh = true)
    }

    fun retry() {
        if (!_state.value.isOnline) {
            viewModelScope.launch {
                _events.send(HomeEvent.Snackbar(R.string.snackbar_refresh_failed))
            }
        }
        refresh()
    }

    fun onScreenResumed(hasFinePermission: Boolean, hasCoarsePermission: Boolean) {
        val current = _state.value
        val hasPermission = hasFinePermission || hasCoarsePermission
        if (!hasPermission && current.location.isDeviceLocation) {
            viewModelScope.launch {
                preferencesRepository.setSelectedLocation(current.defaultCity)
                _events.send(HomeEvent.Snackbar(R.string.home_location_revoked))
            }
            return
        }
        if (hasPermission && current.automaticLocationEnabled &&
            clock.millis() - lastAutoLocateAtMillis > AUTO_LOCATE_INTERVAL_MS
        ) {
            lastAutoLocateAtMillis = clock.millis()
            viewModelScope.launch { locationCoordinator.locate(hasFinePermission, hasCoarsePermission) }
            return
        }
        val snapshot = current.snapshot
        if (snapshot == null || clock.millis() - snapshot.fetchedAtEpochMillis > STALE_REFRESH_MS) {
            loadWeather(current.location, forceRefresh = false)
        }
    }

    fun locateNow(hasFinePermission: Boolean) {
        viewModelScope.launch {
            locationCoordinator.locate(hasFinePermission, !hasFinePermission)
        }
    }

    fun onPermissionResult(fineGranted: Boolean, coarseGranted: Boolean) {
        if (fineGranted || coarseGranted) {
            viewModelScope.launch { locationCoordinator.locate(fineGranted, coarseGranted) }
        } else {
            locationCoordinator.onPermissionDenied(permanentDenial = false)
        }
    }

    fun onPermissionDenied(permanentDenial: Boolean) {
        locationCoordinator.onPermissionDenied(permanentDenial)
        viewModelScope.launch {
            _events.send(
                HomeEvent.Snackbar(
                    messageRes = R.string.snackbar_location_denied,
                    actionLabelRes = R.string.action_open_settings,
                    action = SnackbarAction.OPEN_APP_SETTINGS,
                ),
            )
        }
    }

    fun resetLocationState() {
        locationCoordinator.reset()
    }

    fun toggleSaved(displayName: String) {
        val location = _state.value.location
        viewModelScope.launch {
            if (_state.value.isSaved) {
                val saved = savedCitiesRepository.savedCities.first()
                    .firstOrNull { it.location.isSamePlaceAs(location) }
                if (saved != null) {
                    savedCitiesRepository.remove(saved.location.id)
                    _events.send(
                        HomeEvent.Snackbar(
                            messageRes = R.string.city_removed_snackbar,
                            args = listOf(displayName),
                        ),
                    )
                }
            } else {
                val added = savedCitiesRepository.add(location, clock.millis())
                if (added) {
                    _events.send(
                        HomeEvent.Snackbar(
                            messageRes = R.string.city_added_snackbar,
                            args = listOf(displayName),
                        ),
                    )
                } else {
                    _events.send(HomeEvent.Snackbar(R.string.snackbar_saved_duplicate))
                }
            }
        }
    }

    companion object {
        private const val STALE_REFRESH_MS = 15 * 60 * 1000L
        private const val AUTO_LOCATE_INTERVAL_MS = 10 * 60 * 1000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    preferencesRepository = container.preferencesRepository,
                    savedCitiesRepository = container.savedCitiesRepository,
                    weatherRepository = container.weatherRepository,
                    locationCoordinator = container.locationCoordinator,
                    networkMonitor = container.networkMonitor,
                    widgetStatusStore = container.widgetRefreshStatusStore,
                    hasWidgets = { WidgetUpdater.hasWidgets(container.context) },
                    clock = container.clock,
                )
            }
        }
    }
}
