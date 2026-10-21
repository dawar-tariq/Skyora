package com.skyora.weather.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skyora.weather.SkyoraApplication
import com.skyora.weather.domain.model.WeatherResult
import com.skyora.weather.notifications.NotificationChannels
import com.skyora.weather.notifications.NotificationDedupeStore
import com.skyora.weather.notifications.NotificationScheduler
import com.skyora.weather.notifications.SkyoraNotifier
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Daily forecast summary.
 *
 * The worker reads the user's settings at run time (not at scheduling time), so
 * changing the city or turning the feature off mid-flight cannot produce a
 * stale notification. After running it schedules the next day's occurrence.
 */
class DailyForecastWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SkyoraApplication ?: return Result.success()
        val container = app.container
        val prefs = container.preferencesRepository.preferences.first()
        val settings = prefs.notifications

        if (!settings.dailyForecastEnabled) return Result.success()

        val location = settings.notificationLocation ?: prefs.selectedLocation
        val snapshot = when (val result = container.weatherRepository.loadWeather(location)) {
            is WeatherResult.Success -> result.snapshot
            is WeatherResult.Failure -> result.cachedSnapshot
        } ?: return Result.retry()

        val zone = snapshot.zoneId()
        val dayKey = "${snapshot.location.id}|${LocalDate.now(zone)}"
        val dedupe = NotificationDedupeStore(applicationContext)
        if (!dedupe.shouldSendDaily(dayKey)) {
            NotificationScheduler.scheduleDaily(applicationContext, settings, zone)
            return Result.success()
        }

        NotificationChannels.ensureCreated(applicationContext)
        if (!SkyoraNotifier.canPost(applicationContext)) return Result.success()

        SkyoraNotifier.post(
            applicationContext,
            SkyoraNotifier.ID_DAILY,
            SkyoraNotifier.dailyForecast(applicationContext, snapshot, prefs.temperatureUnit),
        )
        dedupe.markDailySent(dayKey)
        // Re-arm tomorrow at the same wall-clock time in the forecast's zone.
        NotificationScheduler.scheduleDaily(applicationContext, settings, zone)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "skyora_daily_forecast"
    }
}
