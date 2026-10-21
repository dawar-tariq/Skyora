package com.skyora.weather.widget

import android.content.Context
import android.content.SharedPreferences
import com.skyora.weather.domain.model.Location
import kotlinx.serialization.json.Json

/**
 * Per-widget configuration.
 *
 * Android identifies each placed widget by an appWidgetId; the chosen city is
 * stored against it so several widgets can show different cities and survive
 * process death. Deleting a widget removes its entry ([WidgetProviders.onDeleted]).
 */
class WidgetPreferences(context: Context, private val json: Json) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun locationFor(appWidgetId: Int): Location? =
        prefs.getString(key(appWidgetId), null)
            ?.let { runCatching { json.decodeFromString<Location>(it) }.getOrNull() }

    fun setLocation(appWidgetId: Int, location: Location) {
        prefs.edit().putString(key(appWidgetId), json.encodeToString(Location.serializer(), location)).apply()
    }

    /** Falls back to the selected city when the widget has no explicit config. */
    fun clear(appWidgetId: Int) {
        prefs.edit().remove(key(appWidgetId)).apply()
    }

    fun allWidgetIds(): Set<Int> = prefs.all.keys
        .mapNotNull { it.removePrefix(PREFIX).toIntOrNull() }
        .toSet()

    private fun key(appWidgetId: Int) = "$PREFIX$appWidgetId"

    private companion object {
        const val FILE_NAME = "skyora_widgets"
        const val PREFIX = "widget_location_"
    }
}
