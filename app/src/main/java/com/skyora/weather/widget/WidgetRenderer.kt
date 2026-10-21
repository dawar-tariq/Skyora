package com.skyora.weather.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.skyora.weather.MainActivity
import com.skyora.weather.R
import com.skyora.weather.core.util.UpdateAge
import com.skyora.weather.core.util.encodeForRoute
import com.skyora.weather.core.util.formatHour
import com.skyora.weather.core.util.formatTemperature
import com.skyora.weather.core.util.updateAge
import com.skyora.weather.di.AppContainer
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.SkyMoments
import com.skyora.weather.domain.model.SkyPhase
import com.skyora.weather.domain.model.TemperatureUnit
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.ui.designsystem.weatherDescriptionRes
import com.skyora.weather.ui.designsystem.weatherIconRes
import com.skyora.weather.domain.model.WeatherResult
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Builds the RemoteViews for both widget sizes.
 *
 * All values come from the same repository the app uses, so a widget can never
 * disagree with the forecast on screen. Layout adapts to the measured cell
 * width: narrow cells drop the fourth hourly slot and the high/low line rather
 * than clipping them.
 */
object WidgetRenderer {

    private const val NARROW_DP = 220
    private const val SHORT_DP = 130
    private const val SHOW_FOURTH_HOUR_DP = 280

    suspend fun update(
        context: Context,
        container: AppContainer,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        isForecast: Boolean,
    ) {
        val prefs = container.preferencesRepository.preferences.first()
        val widgetPrefs = WidgetPreferences(context.applicationContext, container.json)
        val location = widgetPrefs.locationFor(appWidgetId) ?: prefs.selectedLocation
        val unit = prefs.temperatureUnit

        val snapshot = container.weatherRepository.cachedWeather(location.id)
            ?: (container.weatherRepository.loadWeather(location) as? WeatherResult.Success)?.snapshot

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)

        val views = if (isForecast) {
            forecastViews(context, snapshot, location, unit, minWidth)
        } else {
            compactViews(context, snapshot, location, unit, minWidth, minHeight)
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", plateFor(snapshot))

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_LOCATION_PAYLOAD, location.encodeForRoute())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            appWidgetId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun compactViews(
        context: Context,
        snapshot: WeatherSnapshot?,
        location: Location,
        unit: TemperatureUnit,
        minWidth: Int,
        minHeight: Int,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        views.setTextViewText(R.id.widget_city, location.name.uppercase())

        if (snapshot == null) return emptyState(context, views)

        val current = snapshot.current
        val description = context.getString(weatherDescriptionRes(current.weatherCode))
        views.setTextViewText(R.id.widget_temperature, formatTemperature(current.temperatureC, unit))
        views.setTextViewText(R.id.widget_condition, description)
        views.setImageViewResource(R.id.widget_icon, weatherIconRes(snapshot))

        val today = snapshot.today()
        // Drop the optional line in small cells instead of letting it clip.
        if (today != null && minWidth >= NARROW_DP && minHeight >= SHORT_DP) {
            views.setTextViewText(
                R.id.widget_high_low,
                context.getString(
                    R.string.widget_high_low,
                    formatTemperature(today.tempMaxC, unit),
                    formatTemperature(today.tempMinC, unit),
                ),
            )
            views.setViewVisibility(R.id.widget_high_low, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_high_low, View.GONE)
        }

        views.setTextViewText(R.id.widget_updated, updatedLabel(context, snapshot))
        return views
    }

    private fun forecastViews(
        context: Context,
        snapshot: WeatherSnapshot?,
        location: Location,
        unit: TemperatureUnit,
        minWidth: Int,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_forecast)
        views.setTextViewText(R.id.widget_city, location.name)

        if (snapshot == null) {
            views.setTextViewText(R.id.widget_temperature, context.getString(R.string.widget_no_data))
            views.setTextViewText(R.id.widget_condition, context.getString(R.string.widget_no_data_body))
            views.setImageViewResource(R.id.widget_icon, R.drawable.weather_unknown)
            views.setViewVisibility(R.id.widget_high_low, View.GONE)
            views.setViewVisibility(R.id.hour_4_group, View.GONE)
            views.setTextViewText(R.id.widget_updated, context.getString(R.string.widget_tap_to_open))
            return views
        }

        val current = snapshot.current
        views.setTextViewText(R.id.widget_temperature, formatTemperature(current.temperatureC, unit))
        views.setTextViewText(
            R.id.widget_condition,
            context.getString(weatherDescriptionRes(current.weatherCode)),
        )
        views.setImageViewResource(R.id.widget_icon, weatherIconRes(snapshot))

        val today = snapshot.today()
        if (today != null) {
            views.setTextViewText(
                R.id.widget_high_low,
                context.getString(
                    R.string.widget_high_low,
                    formatTemperature(today.tempMaxC, unit),
                    formatTemperature(today.tempMinC, unit),
                ),
            )
            views.setViewVisibility(R.id.widget_high_low, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_high_low, View.GONE)
        }

        // Hourly slots: skip the leading "now" bucket so the strip shows what's next.
        val hours = snapshot.hourly.drop(1).take(4)
        val zone = snapshot.zoneId()
        val slotIds = listOf(
            Triple(R.id.hour_1_label, R.id.hour_1_icon, R.id.hour_1_temp),
            Triple(R.id.hour_2_label, R.id.hour_2_icon, R.id.hour_2_temp),
            Triple(R.id.hour_3_label, R.id.hour_3_icon, R.id.hour_3_temp),
            Triple(R.id.hour_4_label, R.id.hour_4_icon, R.id.hour_4_temp),
        )
        slotIds.forEachIndexed { index, (labelId, iconId, tempId) ->
            val hour = hours.getOrNull(index)
            if (hour == null) {
                views.setViewVisibility(labelId, View.INVISIBLE)
                views.setViewVisibility(iconId, View.INVISIBLE)
                views.setViewVisibility(tempId, View.INVISIBLE)
                return@forEachIndexed
            }
            views.setViewVisibility(labelId, View.VISIBLE)
            views.setViewVisibility(iconId, View.VISIBLE)
            views.setViewVisibility(tempId, View.VISIBLE)
            views.setTextViewText(
                labelId,
                formatHour(Instant.ofEpochMilli(hour.timeEpochMillis), zone),
            )
            views.setImageViewResource(iconId, weatherIconRes(snapshot, hour))
            views.setTextViewText(tempId, formatTemperature(hour.temperatureC, unit))
        }

        views.setViewVisibility(
            R.id.hour_4_group,
            if (minWidth >= SHOW_FOURTH_HOUR_DP && hours.size >= 4) View.VISIBLE else View.GONE,
        )
        views.setTextViewText(R.id.widget_updated, updatedLabel(context, snapshot))
        return views
    }

    private fun emptyState(context: Context, views: RemoteViews): RemoteViews {
        views.setTextViewText(R.id.widget_temperature, context.getString(R.string.widget_no_data))
        views.setTextViewText(R.id.widget_condition, context.getString(R.string.widget_no_data_body))
        views.setTextViewText(R.id.widget_high_low, "")
        views.setViewVisibility(R.id.widget_high_low, View.GONE)
        views.setImageViewResource(R.id.widget_icon, R.drawable.weather_unknown)
        views.setTextViewText(R.id.widget_updated, context.getString(R.string.widget_tap_to_open))
        return views
    }

    /**
     * Widget plates follow the same [SkyPhase] as the app, but stay dark navy:
     * a home-screen widget sits on an unknown wallpaper, so a full daylight
     * sky would destroy its contrast. Only the plate's top edge and border
     * carry the phase colour.
     */
    private fun plateFor(snapshot: WeatherSnapshot?): Int {
        val moment = snapshot?.let { SkyMoments.resolve(it) } ?: return R.drawable.widget_background
        return when (moment.phase) {
            SkyPhase.DAY -> R.drawable.widget_background_day
            SkyPhase.DAWN, SkyPhase.DUSK -> R.drawable.widget_background_dusk
            SkyPhase.NIGHT -> R.drawable.widget_background
        }
    }

    /** "Updated 12 min ago" — the widget never pretends data is live. */
    fun updatedLabel(context: Context, snapshot: WeatherSnapshot): String {
        val age = updateAge(snapshot.fetchedAtEpochMillis, System.currentTimeMillis())
        val text = when (age) {
            UpdateAge.JustNow -> context.getString(R.string.widget_updated_just_now)
            is UpdateAge.Minutes -> context.getString(R.string.widget_updated_minutes, age.minutes)
            is UpdateAge.Hours -> context.getString(R.string.widget_updated_hours, age.hours)
            UpdateAge.Older -> context.getString(R.string.widget_updated_older)
        }
        return context.getString(R.string.widget_updated_prefix, text)
    }
}
