package com.skyora.weather.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Notification channels.
 *
 * Two channels only:
 *  - DAILY_FORECAST: one scheduled summary per day at the user's chosen time.
 *  - RAIN_ALERT: forecast-based "rain likely" hints. Explicitly not an official
 *    severe-weather warning feed, so it gets its own channel and importance.
 *
 * There is no "severe weather" channel because no official alert source is
 * integrated; creating one would imply a capability the app does not have.
 */
object NotificationChannels {

    const val DAILY_FORECAST = "skyora_daily_forecast"
    const val RAIN_ALERT = "skyora_rain_alert"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val daily = NotificationChannel(
            DAILY_FORECAST,
            context.getString(com.skyora.weather.R.string.notif_channel_daily),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(com.skyora.weather.R.string.notif_channel_daily_desc)
            setShowBadge(true)
        }

        val rain = NotificationChannel(
            RAIN_ALERT,
            context.getString(com.skyora.weather.R.string.notif_channel_rain),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(com.skyora.weather.R.string.notif_channel_rain_desc)
        }

        manager.createNotificationChannels(listOf(daily, rain))
    }
}
