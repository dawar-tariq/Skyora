package com.skyora.weather.domain.model

/**
 * Chooses which hourly slots to show and which one counts as "now".
 *
 * Both the Home strip and the widgets need this, and both previously assumed
 * `hourly[0]` was the current hour. That only holds for a snapshot fetched
 * minutes ago: on cached data the list starts in the past, so the UI labelled an
 * old hour "Now" and showed the wrong day/night artwork for it.
 */
object HourlyWindow {

    /**
     * Index of the slot that contains [nowMillis], or -1 when every slot is in
     * the future (a snapshot fetched just before the hour rolls over).
     *
     * Uses `indexOfLast { start <= now }`, so the slot is the one whose hour has
     * begun — never the next one, which would label 17:59 as "6 pm".
     */
    fun currentIndex(hours: List<HourlyForecast>, nowMillis: Long): Int =
        hours.indexOfLast { it.timeEpochMillis <= nowMillis }

    /**
     * Upcoming slots starting at the hour in progress, capped at [limit].
     *
     * When the whole forecast is in the future (index -1) the list simply starts
     * at its first entry; when it is entirely in the past the result is empty,
     * and callers fall back to their empty state rather than showing stale hours.
     */
    fun upcoming(
        hours: List<HourlyForecast>,
        nowMillis: Long,
        limit: Int,
    ): List<HourlyForecast> {
        if (hours.isEmpty() || limit <= 0) return emptyList()
        val index = currentIndex(hours, nowMillis).let { if (it < 0) 0 else it }
        return hours.drop(index).take(limit)
    }
}
