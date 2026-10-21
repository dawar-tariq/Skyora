package com.skyora.weather.notifications

import android.content.Context
import androidx.core.content.edit

/**
 * Remembers what has already been announced so a 30-minute rain poll cannot
 * spam the same forecast event, and the daily summary fires once per day even
 * if the worker is retried.
 *
 * Keys are calendar-day based (daily) and forecast-hour based (rain), so a
 * genuine new event the following day still notifies.
 */
class NotificationDedupeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun shouldSendDaily(dayKey: String): Boolean = prefs.getString(KEY_DAILY, null) != dayKey

    fun markDailySent(dayKey: String) {
        prefs.edit { putString(KEY_DAILY, dayKey) }
    }

    fun shouldSendRain(eventKey: String): Boolean = prefs.getString(KEY_RAIN, null) != eventKey

    fun markRainSent(eventKey: String) {
        prefs.edit { putString(KEY_RAIN, eventKey) }
    }

    /** Called when the user disables a type so re-enabling can notify again. */
    fun clearRain() {
        prefs.edit { remove(KEY_RAIN) }
    }

    fun clearAll() {
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS = "skyora_notification_state"
        const val KEY_DAILY = "last_daily_day"
        const val KEY_RAIN = "last_rain_event"
    }
}
