package com.skyora.weather.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.NotificationSettings
import com.skyora.weather.work.DailyForecastWorker
import com.skyora.weather.work.RainCheckWorker
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Owns the notification schedules.
 *
 * Design notes:
 *  - Every schedule uses a **unique work name**, so toggling a setting or
 *    changing the delivery time replaces the old job instead of stacking a
 *    second one.
 *  - The daily summary is a chained one-time request (not a periodic one)
 *    because it must fire at a user-chosen wall-clock time. Android may still
 *    delay it; the notification itself always contains the forecast valid at
 *    delivery.
 *  - Rain checks are periodic (30 min) because WorkManager's minimum periodic
 *    interval is 15 minutes and sub-minute precision is not promised by Android.
 *  - WorkManager persists across reboots, so no BOOT_COMPLETED receiver is
 *    needed, and no exact-alarm permission is requested.
 */
object NotificationScheduler {

    private const val UNIQUE_DAILY = "skyora_daily_forecast"
    private const val UNIQUE_RAIN = "skyora_rain_check"

    /**
     * [zone] is the *city's* zone, so "07:00" means 07:00 where the forecast
     * applies — not 07:00 on the phone, which for a saved city abroad would
     * deliver yesterday's or tomorrow's summary. Falls back to the device zone
     * when the city's zone is not known yet.
     */
    fun sync(
        context: Context,
        settings: NotificationSettings,
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        if (settings.dailyForecastEnabled) {
            scheduleDaily(context, settings, zone)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_DAILY)
        }

        if (settings.rainAlertsEnabled) {
            scheduleRainChecks(context)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_RAIN)
        }
    }

    /** Enqueues the next daily run at the user's chosen time in the city's zone. */
    fun scheduleDaily(
        context: Context,
        settings: NotificationSettings,
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        if (!settings.dailyForecastEnabled) return
        val work = OneTimeWorkRequestBuilder<DailyForecastWorker>()
            .setInitialDelay(
                delayUntil(settings.dailyForecastMinutes, ZonedDateTime.now(zone)),
                TimeUnit.MILLISECONDS,
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_DAILY,
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    private fun scheduleRainChecks(context: Context) {
        val work = PeriodicWorkRequestBuilder<RainCheckWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_RAIN,
            ExistingPeriodicWorkPolicy.UPDATE,
            work,
        )
    }

    /**
     * Milliseconds until the next occurrence of [minutesAfterMidnight] in
     * [now]'s zone. DST-safe: the target is built as a wall-clock time and then
     * resolved to an instant, so a 07:00 delivery stays 07:00 after a clock
     * change instead of drifting by an hour.
     */
    fun delayUntil(minutesAfterMidnight: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        val target = now.toLocalDate()
            .atTime(minutesAfterMidnight / 60, minutesAfterMidnight % 60)
            .atZone(now.zone)
            .let { if (it.isAfter(now)) it else it.plusDays(1) }
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }

    /** The zone a saved city's schedules should follow, if it is known. */
    fun zoneFor(location: Location?): ZoneId? = location?.timezone
        ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
}
