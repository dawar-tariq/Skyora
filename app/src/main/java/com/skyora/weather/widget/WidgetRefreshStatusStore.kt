package com.skyora.weather.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyora.weather.data.local.SkyoraDataStores
import com.skyora.weather.domain.widget.WidgetNudgeRules
import com.skyora.weather.domain.widget.WidgetNudgeState
import com.skyora.weather.domain.widget.WidgetRefreshOutcome
import com.skyora.weather.domain.widget.WidgetRefreshStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Tiny persistent record of widget refresh attempts.
 *
 * Deliberately separate from the weather cache: clearing cached forecasts must
 * not erase the evidence that the background job ran (or stopped running).
 */
class WidgetRefreshStatusStore(
    private val dataStore: DataStore<Preferences>,
) {
    val status: Flow<WidgetRefreshStatus> = dataStore.data.map { prefs ->
        WidgetRefreshStatus(
            outcome = prefs[OUTCOME]?.let { value ->
                runCatching { WidgetRefreshOutcome.valueOf(value) }.getOrNull()
            } ?: WidgetRefreshOutcome.NEVER,
            lastAttemptMillis = prefs[LAST_ATTEMPT] ?: 0L,
            lastSuccessMillis = prefs[LAST_SUCCESS] ?: 0L,
        )
    }

    val guideDismissed: Flow<Boolean> = dataStore.data.map { it[GUIDE_DISMISSED] ?: false }

    /** What the Home banner has already been told, so it does not repeat itself. */
    val nudge: Flow<WidgetNudgeState> = dataStore.data.map { prefs ->
        WidgetNudgeState(
            dismissedAtMillis = prefs[NUDGE_DISMISSED_AT] ?: 0L,
            dismissedMarker = prefs[NUDGE_DISMISSED_MARKER] ?: Long.MIN_VALUE,
        )
    }

    suspend fun recordSuccess(nowMillis: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_ATTEMPT] = nowMillis
            prefs[LAST_SUCCESS] = nowMillis
            prefs[OUTCOME] = WidgetRefreshOutcome.SUCCESS.name
        }
    }

    suspend fun recordFailure(nowMillis: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_ATTEMPT] = nowMillis
            prefs[OUTCOME] = WidgetRefreshOutcome.FAILED.name
        }
    }

    suspend fun setGuideDismissed(dismissed: Boolean) {
        dataStore.edit { prefs -> prefs[GUIDE_DISMISSED] = dismissed }
    }

    /**
     * Records that the user dismissed the "widget isn't updating" banner, along
     * with the episode it belonged to, so the same episode stays quiet.
     */
    suspend fun dismissNudge(nowMillis: Long) {
        // Read the episode inside the body (a suspend call in a default
        // argument would not compile).
        val marker = WidgetNudgeRules.episodeMarker(status.first())
        dataStore.edit { prefs ->
            prefs[NUDGE_DISMISSED_AT] = nowMillis
            prefs[NUDGE_DISMISSED_MARKER] = marker
        }
    }

    companion object {
        private val OUTCOME = stringPreferencesKey("widget_refresh_outcome")
        private val LAST_ATTEMPT = longPreferencesKey("widget_refresh_last_attempt")
        private val LAST_SUCCESS = longPreferencesKey("widget_refresh_last_success")
        private val GUIDE_DISMISSED = booleanPreferencesKey("widget_guide_dismissed")
        private val NUDGE_DISMISSED_AT = longPreferencesKey("widget_nudge_dismissed_at")
        private val NUDGE_DISMISSED_MARKER = longPreferencesKey("widget_nudge_dismissed_marker")

        fun create(context: Context): WidgetRefreshStatusStore =
            WidgetRefreshStatusStore(SkyoraDataStores.widgetStatus(context))
    }
}
