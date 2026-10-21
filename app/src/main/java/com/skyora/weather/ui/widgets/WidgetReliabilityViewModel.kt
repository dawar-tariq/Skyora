package com.skyora.weather.ui.widgets

import android.app.ActivityManager
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.widget.DeviceVendor
import com.skyora.weather.domain.widget.DeviceVendorRules
import com.skyora.weather.domain.widget.WidgetRefreshHealth
import com.skyora.weather.domain.widget.WidgetRefreshHealthRules
import com.skyora.weather.domain.widget.WidgetRefreshStatus
import com.skyora.weather.widget.WidgetRefreshStatusStore
import com.skyora.weather.widget.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class WidgetReliabilityState(
    val status: WidgetRefreshStatus = WidgetRefreshStatus(),
    val health: WidgetRefreshHealth = WidgetRefreshHealth.NEVER_RAN,
    val periodicWorkState: WorkInfo.State? = null,
    /** `null` when the platform does not expose the answer. */
    val batteryUnrestricted: Boolean? = null,
    /** `null` when the platform does not expose the answer. */
    val backgroundRestricted: Boolean? = null,
    val vendor: DeviceVendor = DeviceVendor.OTHER,
    val guideDismissed: Boolean = false,
    val widgetCount: Int = 0,
    val isRefreshing: Boolean = false,
    val nowMillis: Long = 0L,
) {
    val intervalMinutes: Long get() = WidgetUpdater.PERIOD_MINUTES
}

class WidgetReliabilityViewModel(
    private val container: AppContainer,
    private val statusStore: WidgetRefreshStatusStore,
    private val workManager: WorkManager,
) : ViewModel() {

    /** Everything the platform can actually tell us about background limits. */
    private data class DeviceFacts(
        val batteryUnrestricted: Boolean?,
        val backgroundRestricted: Boolean?,
        val vendor: DeviceVendor,
        val widgetCount: Int,
    )

    private val statusFlow = statusStore.status.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = WidgetRefreshStatus(),
    )

    private val _state = MutableStateFlow(WidgetReliabilityState(nowMillis = System.currentTimeMillis()))
    val state: StateFlow<WidgetReliabilityState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            statusFlow.collect { status -> publish(status) }
        }
        viewModelScope.launch {
            statusStore.guideDismissed.collect { dismissed ->
                _state.value = _state.value.copy(guideDismissed = dismissed)
            }
        }
        refresh()
    }

    /** Re-reads the work state and system restrictions; safe to call on resume. */
    fun refresh() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val facts = withContext(Dispatchers.Default) { readDeviceFacts() }
            val workState = readPeriodicWorkState()
            _state.value = _state.value.copy(
                status = statusFlow.value,
                health = WidgetRefreshHealthRules.classify(
                    status = statusFlow.value,
                    nowMillis = now,
                    requestedIntervalMinutes = WidgetUpdater.PERIOD_MINUTES,
                ),
                periodicWorkState = workState,
                batteryUnrestricted = facts.batteryUnrestricted,
                backgroundRestricted = facts.backgroundRestricted,
                vendor = facts.vendor,
                widgetCount = facts.widgetCount,
                nowMillis = now,
            )
        }
    }

    /**
     * Runs a refresh now and reports how many widgets were updated. This is the
     * manual escape hatch for the case where an OEM has delayed the background
     * job past the point where the user wants to wait.
     */
    fun refreshWidgetsNow(onResult: (updated: Int) -> Unit) {
        if (_state.value.isRefreshing) return
        _state.value = _state.value.copy(isRefreshing = true)
        viewModelScope.launch {
            val ids = withContext(Dispatchers.IO) {
                WidgetUpdater.allWidgetIds(
                    container.context,
                    android.appwidget.AppWidgetManager.getInstance(container.context)
                        ?: return@withContext IntArray(0),
                )
            }
            if (ids.isNotEmpty()) {
                WidgetUpdater.refresh(container.context, ids)
                // The worker does the fetching; give it a moment before the
                // screen re-reads the journal so "just now" is accurate.
                delay(1_200)
            }
            _state.value = _state.value.copy(isRefreshing = false)
            refresh()
            onResult(ids.size)
        }
    }

    fun dismissGuide() {
        viewModelScope.launch { statusStore.setGuideDismissed(true) }
    }

    fun restoreGuide() {
        viewModelScope.launch { statusStore.setGuideDismissed(false) }
    }

    private fun publish(status: WidgetRefreshStatus) {
        _state.value = _state.value.copy(
            status = status,
            health = WidgetRefreshHealthRules.classify(
                status = status,
                nowMillis = System.currentTimeMillis(),
                requestedIntervalMinutes = WidgetUpdater.PERIOD_MINUTES,
            ),
        )
    }

    private suspend fun readPeriodicWorkState(): WorkInfo.State? {
        val future = workManager.getWorkInfosForUniqueWork(WidgetUpdater.UNIQUE_PERIODIC)
        val infos: List<WorkInfo>? = try {
            suspendCancellableCoroutine<List<WorkInfo>?> { continuation ->
                future.addListener(
                    {
                        // The listener only fires once the future is complete,
                        // so this get() does not block.
                        val value = runCatching { future.get() }.getOrNull()
                        if (continuation.isActive) continuation.resume(value)
                    },
                    { runnable -> runnable.run() },
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            null
        }
        return infos?.lastOrNull()?.state
    }

    private fun readDeviceFacts(): DeviceFacts {
        val context = container.context
        val power = context.getSystemService(PowerManager::class.java)
        val activity = context.getSystemService(ActivityManager::class.java)
        val unrestricted = power?.let {
            runCatching { it.isIgnoringBatteryOptimizations(context.packageName) }.getOrNull()
        }
        val backgroundRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity?.let { runCatching { it.isBackgroundRestricted }.getOrNull() }
        } else {
            null
        }
        val widgetCount = android.appwidget.AppWidgetManager.getInstance(context)?.let { manager ->
            WidgetUpdater.allWidgetIds(context, manager).size
        } ?: 0
        return DeviceFacts(
            batteryUnrestricted = unrestricted,
            backgroundRestricted = backgroundRestricted,
            vendor = DeviceVendorRules.detect(Build.MANUFACTURER ?: "", Build.BRAND ?: ""),
            widgetCount = widgetCount,
        )
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WidgetReliabilityViewModel(
                    container = container,
                    statusStore = container.widgetRefreshStatusStore,
                    workManager = WorkManager.getInstance(container.context),
                )
            }
        }
    }
}
