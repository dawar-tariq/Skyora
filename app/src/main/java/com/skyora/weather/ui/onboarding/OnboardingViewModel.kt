package com.skyora.weather.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.UserPreferences
import com.skyora.weather.domain.repository.PreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Preferences shown on the notification step of onboarding.
 *
 * The step writes the same preference keys the Settings screen writes, so the
 * scheduled work is produced by the normal [NotificationScheduler] observer in
 * `AppContainer` — there is no second, onboarding-only scheduling path.
 */
class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _completed = Channel<Unit>(Channel.BUFFERED)

    /** Emits after the flag is durably written, so the caller can navigate. */
    val completed: Flow<Unit> = _completed.receiveAsFlow()

    /** Current notification choices plus the city they will describe. */
    val notificationState: StateFlow<OnboardingNotificationState> = preferencesRepository.preferences
        .map { prefs -> prefs.toOnboardingState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = OnboardingNotificationState(),
        )

    private var completionRequested = false

    fun enableDailyForecast(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDailyForecastEnabled(enabled) }
    }

    fun setDailyForecastTime(minutesAfterMidnight: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyForecastMinutes(minutesAfterMidnight)
            // Choosing a time is an intent to receive it.
            preferencesRepository.setDailyForecastEnabled(true)
        }
    }

    fun setRainAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setRainAlertsEnabled(enabled) }
    }

    fun setRainThreshold(percent: Int) {
        viewModelScope.launch { preferencesRepository.setRainThresholdPercent(percent) }
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
                OnboardingViewModel(container.preferencesRepository)
            }
        }
    }
}

/** Everything the notification step needs, derived from stored preferences. */
data class OnboardingNotificationState(
    val dailyForecastEnabled: Boolean = false,
    val rainAlertsEnabled: Boolean = false,
    val dailyMinutes: Int = 7 * 60,
    val rainThresholdPercent: Int = 60,
    val cityName: String = "",
) {
    val anyEnabled: Boolean get() = dailyForecastEnabled || rainAlertsEnabled

    /** 7:05 -> "07:05". */
    val timeLabel: String get() = "%02d:%02d".format(dailyMinutes / 60, dailyMinutes % 60)
}

private fun UserPreferences.toOnboardingState(): OnboardingNotificationState =
    OnboardingNotificationState(
        dailyForecastEnabled = notifications.dailyForecastEnabled,
        rainAlertsEnabled = notifications.rainAlertsEnabled,
        dailyMinutes = notifications.dailyForecastMinutes,
        rainThresholdPercent = notifications.rainThresholdPercent,
        cityName = (notifications.notificationLocation ?: selectedLocation).name,
    )
