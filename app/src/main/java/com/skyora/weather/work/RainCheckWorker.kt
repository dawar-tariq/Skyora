package com.skyora.weather.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.skyora.weather.SkyoraApplication
import com.skyora.weather.domain.model.WeatherResult
import com.skyora.weather.domain.notifications.RainAlertPolicy
import com.skyora.weather.notifications.NotificationChannels
import com.skyora.weather.notifications.NotificationDedupeStore
import com.skyora.weather.notifications.SkyoraNotifier
import kotlinx.coroutines.flow.first

/**
 * Forecast-based rain alerts.
 *
 * Deliberately conservative:
 *  - uses Open-Meteo's `precipitation_probability` (a real probability, not an
 *    inference from the weather code);
 *  - only looks at the configured horizon and threshold;
 *  - requires the hour to be in the future;
 *  - sends at most one alert per forecast hour (dedupe key), so a 30-minute
 *    poll cycle cannot repeat itself.
 */
class RainCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SkyoraApplication ?: return Result.success()
        val container = app.container
        val prefs = container.preferencesRepository.preferences.first()
        val settings = prefs.notifications

        if (!settings.rainAlertsEnabled) return Result.success()
        if (!SkyoraNotifier.canPost(applicationContext)) return Result.success()

        val location = settings.notificationLocation ?: prefs.selectedLocation
        val snapshot = when (val result = container.weatherRepository.loadWeather(location)) {
            is WeatherResult.Success -> result.snapshot
            is WeatherResult.Failure -> result.cachedSnapshot ?: return Result.retry()
        }

        val match = RainAlertPolicy.firstRainyHour(
            snapshot = snapshot,
            thresholdPercent = settings.rainThresholdPercent,
            lookaheadHours = settings.rainLookaheadHours,
            nowMillis = System.currentTimeMillis(),
        ) ?: return Result.success()

        val next = match.hour
        val probability = match.probability
        val dedupe = NotificationDedupeStore(applicationContext)
        val key = RainAlertPolicy.eventKey(snapshot, next)
        if (!dedupe.shouldSendRain(key)) return Result.success()

        NotificationChannels.ensureCreated(applicationContext)
        SkyoraNotifier.post(
            applicationContext,
            SkyoraNotifier.ID_RAIN,
            SkyoraNotifier.rainAlert(applicationContext, snapshot, next, probability, prefs.temperatureUnit),
        )
        dedupe.markRainSent(key)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "skyora_rain_check"
    }
}
