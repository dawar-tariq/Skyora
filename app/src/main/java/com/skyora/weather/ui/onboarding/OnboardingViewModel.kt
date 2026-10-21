package com.skyora.weather.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.repository.PreferencesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _completed = Channel<Unit>(Channel.BUFFERED)

    /** Emits after the flag is durably written, so the caller can navigate. */
    val completed: Flow<Unit> = _completed.receiveAsFlow()

    private var completionRequested = false

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
