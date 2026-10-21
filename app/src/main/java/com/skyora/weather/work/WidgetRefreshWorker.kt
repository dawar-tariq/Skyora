package com.skyora.weather.work

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skyora.weather.SkyoraApplication
import com.skyora.weather.domain.model.WeatherResult
import com.skyora.weather.widget.CompactWeatherWidgetProvider
import com.skyora.weather.widget.ForecastWeatherWidgetProvider
import com.skyora.weather.widget.WidgetPreferences
import com.skyora.weather.widget.WidgetRenderer
import com.skyora.weather.widget.WidgetUpdater
import kotlinx.coroutines.flow.first

/**
 * Refreshes every placed widget.
 *
 * A widget that already has a cached snapshot is refreshed through the
 * repository's normal cache rules; if the network fails the cached snapshot is
 * re-rendered and its "updated N ago" line tells the user how old it is. A
 * widget with no data at all shows the placeholder rather than zeros.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SkyoraApplication ?: return Result.success()
        val container = app.container
        val manager = AppWidgetManager.getInstance(applicationContext) ?: return Result.success()
        val widgetPrefs = WidgetPreferences(applicationContext, container.json)
        val statusStore = container.widgetRefreshStatusStore

        val compactIds = manager.getAppWidgetIds(
            ComponentName(applicationContext, CompactWeatherWidgetProvider::class.java),
        )
        val forecastIds = manager.getAppWidgetIds(
            ComponentName(applicationContext, ForecastWeatherWidgetProvider::class.java),
        )

        if (compactIds.isEmpty() && forecastIds.isEmpty()) {
            WidgetUpdater.cancelPeriodicRefresh(applicationContext)
            return Result.success()
        }
        // Re-arm the safety net: it may have been cancelled while no widgets existed.
        WidgetUpdater.schedulePeriodicRefresh(applicationContext)

        // Fetch each distinct city once, then render every widget that uses it.
        // A widget without an explicit city follows the selected city.
        // Refreshes triggered by these fetches are suppressed so this worker is
        // not replaced (and cancelled) by its own unique-work request.
        WidgetUpdater.beginInternalRefresh()
        var hadNetworkFailure = false
        try {
            val fallback = container.preferencesRepository.preferences.first().selectedLocation
            val ids = compactIds.toList() + forecastIds.toList()
            val locations = ids.map { id -> id to (widgetPrefs.locationFor(id) ?: fallback) }
            locations.map { it.second }.distinctBy { it.id }.forEach { location ->
                when (container.weatherRepository.loadWeather(location)) {
                    is WeatherResult.Success -> Unit
                    is WeatherResult.Failure -> hadNetworkFailure = true
                }
            }

            compactIds.forEach { id ->
                WidgetRenderer.update(applicationContext, container, manager, id, isForecast = false)
            }
            forecastIds.forEach { id ->
                WidgetRenderer.update(applicationContext, container, manager, id, isForecast = true)
            }
        } finally {
            WidgetUpdater.endInternalRefresh()
        }

        // Journal the outcome so Settings can show whether the background job
        // is actually running, not just whether it was scheduled.
        if (hadNetworkFailure) {
            statusStore.recordFailure(System.currentTimeMillis())
        } else {
            statusStore.recordSuccess(System.currentTimeMillis())
        }

        // Retry with backoff only when a fetch actually failed; cached data was
        // still rendered above.
        return if (hadNetworkFailure) Result.retry() else Result.success()
    }

    companion object {
        const val WORK_NAME = "skyora_widget_refresh_worker"
    }
}
