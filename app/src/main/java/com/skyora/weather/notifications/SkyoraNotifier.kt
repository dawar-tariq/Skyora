package com.skyora.weather.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.skyora.weather.MainActivity
import com.skyora.weather.R
import com.skyora.weather.core.util.encodeForRoute
import com.skyora.weather.core.util.formatHour
import com.skyora.weather.core.util.formatTemperature
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.HourlyForecast
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.ui.designsystem.weatherDescriptionRes
import com.skyora.weather.ui.designsystem.weatherIconRes
import java.time.Instant

/**
 * Builds and posts the two local notification types.
 *
 * Every number shown here comes from the same [WeatherSnapshot] the app
 * renders; nothing is synthesised. Icons come from the shared weather-icon set
 * so a notification matches the hourly card for the same hour.
 */
object SkyoraNotifier {

    /** Free-form category string; the platform has no weather constant. */
    private const val CATEGORY_WEATHER = "weather"

    const val ID_DAILY = 1001
    const val ID_RAIN = 1002

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun dailyForecast(
        context: Context,
        snapshot: WeatherSnapshot,
        unit: TemperatureUnit,
    ): Notification {
        val today = snapshot.today()
        val description = context.getString(weatherDescriptionRes(snapshot.current.weatherCode))
        val high = today?.let { formatTemperature(it.tempMaxC, unit) } ?: "—"
        val low = today?.let { formatTemperature(it.tempMinC, unit) } ?: "—"
        val city = snapshot.location.name

        val title = context.getString(R.string.notif_daily_title, city)
        val text = context.getString(R.string.notif_daily_text, description, high, low)

        return base(context, NotificationChannels.DAILY_FORECAST, snapshot)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
    }

    fun rainAlert(
        context: Context,
        snapshot: WeatherSnapshot,
        hour: HourlyForecast,
        probability: Int,
        unit: TemperatureUnit,
    ): Notification {
        val description = context.getString(weatherDescriptionRes(hour.weatherCode))
        val time = formatHour(Instant.ofEpochMilli(hour.timeEpochMillis), snapshot.zoneId())
        val city = snapshot.location.name
        val title = context.getString(R.string.notif_rain_title, city)
        val text = context.getString(
            R.string.notif_rain_text,
            probability,
            time,
            description,
        )

        return base(context, NotificationChannels.RAIN_ALERT, snapshot)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    private fun base(
        context: Context,
        channel: String,
        snapshot: WeatherSnapshot,
    ): NotificationCompat.Builder {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_LOCATION_PAYLOAD, snapshot.location.encodeForRoute())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            channel.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_weather)
            .setLargeIcon(largeIcon(context, snapshot))
            .setContentIntent(contentIntent)
            .setCategory(CATEGORY_WEATHER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(context.getColor(R.color.skyora_cyan))
            .setShowWhen(false)
    }

    /**
     * Notifications cannot carry a vector large icon, so the artwork is drawn
     * into a bitmap from the same drawable used elsewhere.
     */
    private fun largeIcon(context: Context, snapshot: WeatherSnapshot): android.graphics.Bitmap? =
        runCatching {
            val resId = weatherIconRes(snapshot)
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId) ?: return null
            val size = (context.resources.displayMetrics.density * 64).toInt().coerceAtLeast(96)
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap
        }.getOrNull()

    fun post(context: Context, id: Int, notification: Notification) {
        if (!canPost(context)) return
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

}
