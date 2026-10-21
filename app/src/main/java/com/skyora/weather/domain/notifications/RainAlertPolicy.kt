package com.skyora.weather.domain.notifications

import com.skyora.weather.domain.model.HourlyForecast
import com.skyora.weather.domain.model.WeatherSnapshot

/**
 * Pure decision logic for rain alerts.
 *
 * Kept out of the Worker so it can be unit-tested on the JVM and reused by any
 * future scheduler. The policy is intentionally conservative: it uses the
 * API's `precipitation_probability` field (never the weather code), ignores
 * past hours, and returns at most one hour so a single notification is posted
 * per forecast event.
 */
object RainAlertPolicy {

    data class Match(
        val hour: HourlyForecast,
        val probability: Int,
    )

    fun firstRainyHour(
        snapshot: WeatherSnapshot,
        thresholdPercent: Int,
        lookaheadHours: Int,
        nowMillis: Long,
    ): Match? {
        if (lookaheadHours <= 0) return null
        val horizonEnd = nowMillis + lookaheadHours * 3_600_000L
        val hour = snapshot.hourly.firstOrNull { candidate ->
            val probability = candidate.precipitationProbability ?: return@firstOrNull false
            candidate.timeEpochMillis in nowMillis..horizonEnd && probability >= thresholdPercent
        } ?: return null
        return Match(hour = hour, probability = hour.precipitationProbability ?: 0)
    }

    /** Stable key for deduplication: one alert per location per forecast hour. */
    fun eventKey(snapshot: WeatherSnapshot, hour: HourlyForecast): String =
        "${snapshot.location.id}|${hour.timeEpochMillis / 3_600_000L}"
}
