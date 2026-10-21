package com.skyora.weather.ui.splash

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class StartDestination { ONBOARDING, MAIN }

class SplashViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _destination = MutableStateFlow<StartDestination?>(null)
    val destination: StateFlow<StartDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val onboardingCompleted = withTimeoutOrNull(1_500L) {
                preferencesRepository.preferences.first().onboardingCompleted
            } ?: false
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            delay((MIN_BRAND_MOMENT_MS - elapsed).coerceAtLeast(0L))
            _destination.value = if (onboardingCompleted) StartDestination.MAIN else StartDestination.ONBOARDING
        }
    }

    companion object {
        /** Short, intentional brand reveal — not an artificial loading screen. */
        private const val MIN_BRAND_MOMENT_MS = 850L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SplashViewModel(container.preferencesRepository)
            }
        }
    }
}
