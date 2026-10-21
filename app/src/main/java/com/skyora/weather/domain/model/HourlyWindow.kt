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

    /** Length of one forecast slot; every Open-Meteo hourly series is 1 h. */
    const val SLOT_MILLIS: Long = 60L * 60L * 1000L

    /**
     * Upcoming slots starting at the hour in progress, capped at [limit].
     *
     * When the whole forecast is in the future (index -1) the list starts at its
     * first entry. When the newest slot has already ended — a snapshot from
     * yesterday, or a device that has been offline since — the result is empty:
     * showing that hour as "now" would be reporting stale data as current.
     */
    fun upcoming(
        hours: List<HourlyForecast>,
        nowMillis: Long,
        limit: Int,
    ): List<HourlyForecast> {
        if (hours.isEmpty() || limit <= 0) return emptyList()
        val index = currentIndex(hours, nowMillis)
        if (index < 0) return hours.take(limit)
        val currentSlotEndsAt = hours[index].timeEpochMillis + SLOT_MILLIS
        if (nowMillis >= currentSlotEndsAt) return emptyList()
        return hours.drop(index).take(limit)
    }
}
