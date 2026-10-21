package com.skyora.weather.ui.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.location.LocationCoordinator
import com.skyora.weather.domain.model.LocationAccessState
import com.skyora.weather.domain.repository.PreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LocationPermissionViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val locationCoordinator: LocationCoordinator,
) : ViewModel() {

    val locationState: StateFlow<LocationAccessState> = locationCoordinator.state

    private val _completed = Channel<Unit>(Channel.BUFFERED)

    /** Emits after onboarding completion is persisted. */
    val completed: Flow<Unit> = _completed.receiveAsFlow()

    private var completionRequested = false

    fun locate(hasFinePermission: Boolean) {
        viewModelScope.launch {
            locationCoordinator.locate(hasFinePermission, !hasFinePermission)
        }
    }

    fun onPermissionDenied(permanentDenial: Boolean) {
        locationCoordinator.onPermissionDenied(permanentDenial)
    }

    fun retry(hasFinePermission: Boolean) {
        locationCoordinator.reset()
        locate(hasFinePermission)
    }

    fun completeOnboarding() {
        if (completionRequested) return
        completionRequested = true
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            _completed.send(Unit)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LocationPermissionViewModel(
                    preferencesRepository = container.preferencesRepository,
                    locationCoordinator = container.locationCoordinator,
                )
            }
        }
    }
}
