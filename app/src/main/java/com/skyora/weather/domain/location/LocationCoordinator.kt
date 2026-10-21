package com.skyora.weather.domain.location

import com.skyora.weather.domain.model.DeviceLocationResult
import com.skyora.weather.domain.model.LocationAccessState
import com.skyora.weather.domain.repository.DeviceLocationRepository
import com.skyora.weather.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Single source of truth for the "use my location" action, shared by Home, the
 * permission explainer and Settings. A successful fix becomes the selected
 * location; the device's GPS coordinates are never used as a search or saved
 * city id.
 */
class LocationCoordinator(
    private val deviceLocationRepository: DeviceLocationRepository,
    private val preferencesRepository: PreferencesRepository,
) {
    private val _state = MutableStateFlow<LocationAccessState>(LocationAccessState.Idle)
    val state: StateFlow<LocationAccessState> = _state.asStateFlow()

    private val lock = Mutex()

    suspend fun locate(hasFinePermission: Boolean, hasCoarsePermission: Boolean) {
        if (!hasFinePermission && !hasCoarsePermission) {
            _state.value = LocationAccessState.PermissionRequired(permanentDenial = false)
            return
        }
        if (!lock.tryLock()) return
        try {
            _state.value = LocationAccessState.Locating
            when (val result = deviceLocationRepository.getCurrentLocation(hasFinePermission)) {
                is DeviceLocationResult.Success -> {
                    preferencesRepository.setSelectedLocation(result.location)
                    _state.value = LocationAccessState.Success(result.location, result.isApproximate)
                }

                is DeviceLocationResult.Failure -> {
                    _state.value = LocationAccessState.Error(result.reason)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun onPermissionDenied(permanentDenial: Boolean) {
        _state.value = LocationAccessState.PermissionRequired(permanentDenial)
    }

    fun reset() {
        if (_state.value !is LocationAccessState.Locating) {
            _state.value = LocationAccessState.Idle
        }
    }
}
