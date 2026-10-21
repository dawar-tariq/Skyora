package com.skyora.weather.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.skyora.weather.core.network.NetworkMonitor
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.domain.repository.CitySearchResult
import com.skyora.weather.domain.repository.GeocodingRepository
import com.skyora.weather.domain.repository.PreferencesRepository
import com.skyora.weather.domain.repository.SavedCitiesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

enum class SearchMode { BROWSE, PICK_HOME, ADD_CITY }

sealed interface SearchEvent {
    /** Sent only after the selection has been persisted. */
    data class CitySelected(val location: Location) : SearchEvent
}

data class SearchUiState(
    val mode: SearchMode = SearchMode.BROWSE,
    val query: String = "",
    val submittedQuery: String = "",
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val results: List<Location> = emptyList(),
    val recentSearches: List<Location> = emptyList(),
    val error: WeatherError? = null,
    val isOnline: Boolean = true,
)

private data class SearchRequest(val query: String, val token: Int = 0)

private sealed interface SearchOutcome {
    data class Idle(val query: String) : SearchOutcome
    data class Loading(val query: String) : SearchOutcome
    data class Success(val query: String, val results: List<Location>) : SearchOutcome
    data class Failure(val query: String, val error: WeatherError) : SearchOutcome
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val geocodingRepository: GeocodingRepository,
    private val preferencesRepository: PreferencesRepository,
    private val savedCitiesRepository: SavedCitiesRepository,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
    private val mode: SearchMode,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState(mode = mode))
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _events = Channel<SearchEvent>(Channel.BUFFERED)
    val events: Flow<SearchEvent> = _events.receiveAsFlow()

    private val queryInput = MutableStateFlow("")
    private val retrySignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var retryToken = 0

    init {
        observeRecentSearches()
        observeConnectivity()
        observeSearchRequests()
    }

    private fun observeRecentSearches() {
        viewModelScope.launch {
            preferencesRepository.preferences
                .map { it.recentSearches }
                .distinctUntilChanged()
                .collect { recents ->
                    _state.update { it.copy(recentSearches = recents) }
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

    private fun observeSearchRequests() {
        viewModelScope.launch {
            val typed = queryInput
                .debounce(DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .map { SearchRequest(query = it) }
            val retries = retrySignal.map {
                SearchRequest(query = queryInput.value.trim(), token = ++retryToken)
            }
            merge(typed, retries)
                .flatMapLatest { request -> searchFlow(request.query) }
                .collect { outcome -> applyOutcome(outcome) }
        }
    }

    private fun searchFlow(rawQuery: String): Flow<SearchOutcome> = flow {
        val query = rawQuery.trim()
        if (query.length < MIN_QUERY_LENGTH) {
            emit(SearchOutcome.Idle(query))
            return@flow
        }
        emit(SearchOutcome.Loading(query))
        if (!networkMonitor.isOnline.value) {
            emit(SearchOutcome.Failure(query, WeatherError.Offline))
            return@flow
        }
        when (val result = geocodingRepository.searchCities(query)) {
            is CitySearchResult.Success -> emit(SearchOutcome.Success(query, result.locations))
            is CitySearchResult.Failure -> emit(SearchOutcome.Failure(query, result.error))
        }
    }

    private fun applyOutcome(outcome: SearchOutcome) {
        _state.update { current ->
            when (outcome) {
                is SearchOutcome.Idle -> current.copy(
                    isSearching = false,
                    hasSearched = false,
                    results = emptyList(),
                    error = null,
                    submittedQuery = outcome.query,
                )

                is SearchOutcome.Loading -> current.copy(
                    isSearching = true,
                    hasSearched = false,
                    results = emptyList(),
                    error = null,
                    submittedQuery = outcome.query,
                )

                is SearchOutcome.Success -> current.copy(
                    isSearching = false,
                    hasSearched = true,
                    results = outcome.results,
                    error = null,
                    submittedQuery = outcome.query,
                )

                is SearchOutcome.Failure -> current.copy(
                    isSearching = false,
                    hasSearched = true,
                    results = emptyList(),
                    error = outcome.error,
                    submittedQuery = outcome.query,
                )
            }
        }
    }

    fun onQueryChange(value: String) {
        queryInput.value = value
        _state.update { current ->
            current.copy(
                query = value,
                error = null,
                hasSearched = false,
                isSearching = false,
                results = if (value.isBlank()) emptyList() else current.results,
                submittedQuery = value.trim(),
            )
        }
    }

    fun clearQuery() = onQueryChange("")

    fun retry() {
        val query = queryInput.value.trim()
        if (query.length < MIN_QUERY_LENGTH) return
        _state.update { it.copy(isSearching = true, error = null) }
        retrySignal.tryEmit(Unit)
    }

    /**
     * Handles a result tap: remembers the city, then applies the mode-specific
     * action. Saving here (rather than in a composable) keeps persistence out
     * of the UI layer and means an "add city" search returns a saved city when
     * the caller navigates back.
     */
    fun onCitySelected(location: Location) {
        viewModelScope.launch {
            preferencesRepository.addRecentSearch(location)
            when (mode) {
                SearchMode.BROWSE -> Unit
                SearchMode.PICK_HOME -> preferencesRepository.setHomeCity(location)
                SearchMode.ADD_CITY -> savedCitiesRepository.add(location, clock.millis())
            }
            _events.send(SearchEvent.CitySelected(location))
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { preferencesRepository.clearRecentSearches() }
    }

    companion object {
        private const val DEBOUNCE_MS = 350L
        private const val MIN_QUERY_LENGTH = 2

        fun factory(container: AppContainer, mode: SearchMode): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SearchViewModel(
                        geocodingRepository = container.geocodingRepository,
                        preferencesRepository = container.preferencesRepository,
                        savedCitiesRepository = container.savedCitiesRepository,
                        networkMonitor = container.networkMonitor,
                        clock = container.clock,
                        mode = mode,
                    )
                }
            }
    }
}
