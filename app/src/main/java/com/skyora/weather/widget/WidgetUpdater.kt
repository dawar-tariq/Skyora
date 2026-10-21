package com.skyora.weather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skyora.weather.work.WidgetRefreshWorker
import java.util.concurrent.TimeUnit

/**
 * Entry point for every widget refresh.
 *
 * Widgets are refreshed
 *  - when one is placed, resized or re-added ([SkyoraWidgetProvider]),
 *  - after any successful weather fetch (repository callback in AppContainer),
 *  - when the selected city / unit / notification city changes (preference
 *    observer in AppContainer),
 *  - and periodically in the background ([schedulePeriodicRefresh]).
 *
 * All work is coalesced under a single unique name so a burst of triggers
 * produces one fetch, not six.
 */
object WidgetUpdater {

    private const val UNIQUE_REFRESH = "skyora_widget_refresh"
    private const val UNIQUE_PERIODIC = "skyora_widget_periodic"
    private const val PERIOD_MINUTES = 60L

    /**
     * Set while [com.skyora.weather.work.WidgetRefreshWorker] is fetching and
     * rendering. A fetch stores a snapshot, which normally triggers a refresh;
     * re-enqueueing the same unique work from inside it would cancel the
     * running worker before it renders, so that callback is ignored here.
     */
    @Volatile
    private var internalRefreshRunning = false

    fun beginInternalRefresh() {
        internalRefreshRunning = true
    }

    fun endInternalRefresh() {
        internalRefreshRunning = false
    }

    fun refreshNow(context: Context) {
        if (internalRefreshRunning) return
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = allWidgetIds(context, manager)
        if (ids.isEmpty()) return
        refresh(context, ids)
    }

    fun refresh(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val network = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_REFRESH,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * Periodic safety net so widgets age gracefully even if the app is never
     * opened. Android batches this work; it is not an exact-time alarm.
     */
    fun schedulePeriodicRefresh(context: Context) {
        val network = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            PERIOD_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelPeriodicRefresh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
    }

    fun allWidgetIds(context: Context, manager: AppWidgetManager): IntArray =
        manager.getAppWidgetIds(ComponentName(context, CompactWeatherWidgetProvider::class.java)) +
            manager.getAppWidgetIds(ComponentName(context, ForecastWeatherWidgetProvider::class.java))
}
