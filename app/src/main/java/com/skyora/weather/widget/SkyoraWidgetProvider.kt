package com.skyora.weather.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.skyora.weather.SkyoraApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Base provider shared by both widgets.
 *
 * Updates are always delegated to [WidgetUpdater], which enqueues coalesced
 * WorkManager jobs. Doing network work directly in `onUpdate` would exceed the
 * BroadcastReceiver time budget and would be killed on slow connections.
 */
abstract class SkyoraWidgetProvider : AppWidgetProvider() {

    /** Which layout family this provider serves. */
    abstract val isForecast: Boolean

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext as SkyoraApplication
        val widgetPrefs = WidgetPreferences(context, app.container.json)
        val selected = runBlocking {
            app.container.preferencesRepository.preferences.first()
        }.selectedLocation

        appWidgetIds.forEach { id ->
            // First placement: adopt the currently selected city so the widget
            // shows something immediately instead of an empty placeholder.
            if (widgetPrefs.locationFor(id) == null && selected != null) {
                widgetPrefs.setLocation(id, selected)
            }
        }
        WidgetUpdater.refresh(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // A resize changes how many hourly slots fit, so re-render this instance.
        WidgetUpdater.refresh(context, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val app = context.applicationContext as SkyoraApplication
        val widgetPrefs = WidgetPreferences(context, app.container.json)
        appWidgetIds.forEach(widgetPrefs::clear)
    }
}

/** Widget A — 2x2 compact current conditions. */
class CompactWeatherWidgetProvider : SkyoraWidgetProvider() {
    override val isForecast: Boolean = false
}

/** Widget B — 4x2 forecast with the next hours. */
class ForecastWeatherWidgetProvider : SkyoraWidgetProvider() {
    override val isForecast: Boolean = true
}
