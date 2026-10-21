package com.skyora.weather.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.skyora.weather.R
import com.skyora.weather.SkyoraApplication
import com.skyora.weather.domain.model.Location
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * City picker shown when a SKYORA widget is placed.
 *
 * Lists the saved cities plus the currently selected city, and stores the
 * choice against this widget id. Cancelling removes the widget instead of
 * leaving an unconfigured card on the launcher.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as SkyoraApplication
        val container = app.container
        val widgetPrefs = WidgetPreferences(this, container.json)
        val prefs = runBlocking { container.preferencesRepository.preferences.first() }
        val saved = runBlocking { container.savedCitiesRepository.savedCities.first() }

        val candidates = buildList {
            add(prefs.selectedLocation)
            add(prefs.defaultCity)
            saved.forEach { add(it.location) }
        }.distinctBy { it.id }

        val list = findViewById<LinearLayout>(R.id.widget_city_list)
        val empty = findViewById<TextView>(R.id.widget_config_empty)

        candidates.forEach { location ->
            list.addView(cityRow(location) { select(widgetPrefs, location) })
        }
        if (candidates.isEmpty()) empty.visibility = View.VISIBLE

        findViewById<View>(R.id.widget_config_cancel).setOnClickListener {
            finish() // RESULT_CANCELED -> host removes the widget
        }
        findViewById<View>(R.id.widget_config_use_selected).setOnClickListener {
            select(widgetPrefs, prefs.selectedLocation)
        }
    }

    private fun select(widgetPrefs: WidgetPreferences, location: Location) {
        widgetPrefs.setLocation(appWidgetId, location)
        WidgetUpdater.refresh(this, intArrayOf(appWidgetId))
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        finish()
    }

    private fun cityRow(location: Location, onClick: () -> Unit): View {
        val row = TextView(this).apply {
            text = buildString {
                append(location.name)
                location.admin1?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
            }
            setTextColor(getColor(R.color.skyora_text_primary_dark))
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 30, 28, 30)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        return row
    }
}
