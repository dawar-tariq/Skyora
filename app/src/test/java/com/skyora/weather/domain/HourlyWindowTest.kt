package com.skyora.weather.domain

import com.skyora.weather.domain.model.HourlyForecast
import com.skyora.weather.domain.model.HourlyWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The hourly strip and the widgets must agree about which slot is "now" and
 * which slots are upcoming, especially on cached data where the list starts in
 * the past.
 */
class HourlyWindowTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val hourMs = 3_600_000L

    /** 2026-10-21 [hour]:[minute] IST, rolled over into the next day when needed. */
    private fun at(hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(2026, 10, 21, 0, 0, 0, 0, zone)
            .plusHours(hour.toLong())
            .plusMinutes(minute.toLong())
            .toInstant()
            .toEpochMilli()

    /** Consecutive hourly slots starting at [startHour] (may cross midnight). */
    private fun hours(startHour: Int, count: Int): List<HourlyForecast> =
        (0 until count).map { offset ->
            HourlyForecast(
                timeEpochMillis = at(startHour + offset),
                temperatureC = 10.0 + offset,
                weatherCode = 0,
                isDay = true,
            )
        }

    @Test
    fun `a fresh snapshot labels the hour in progress as now`() {
        val list = hours(startHour = 18, count = 24)
        // 18:16 -> the 18:00 slot is current, not 19:00.
        assertEquals(0, HourlyWindow.currentIndex(list, at(18, 16)))
        assertEquals(list.first(), HourlyWindow.upcoming(list, at(18, 16), 24).first())
    }

    @Test
    fun `just before the hour rolls over the current slot is still now`() {
        val list = hours(startHour = 18, count = 6)
        assertEquals(0, HourlyWindow.currentIndex(list, at(18, 59)))
    }

    @Test
    fun `one minute into the next hour that slot becomes now`() {
        val list = hours(startHour = 18, count = 6)
        assertEquals(1, HourlyWindow.currentIndex(list, at(19, 1)))
        assertEquals(
            at(19),
            HourlyWindow.upcoming(list, at(19, 1), 24).first().timeEpochMillis,
        )
    }

    @Test
    fun `a cached snapshot skips the hours that already passed`() {
        // Fetched at 18:00 and read at 21:30: the strip must start at 21:00.
        val list = hours(startHour = 18, count = 24)
        val upcoming = HourlyWindow.upcoming(list, at(21, 30), 24)
        assertEquals(at(21), upcoming.first().timeEpochMillis)
        assertEquals(21, upcoming.size)
    }

    @Test
    fun `an entirely stale snapshot yields no upcoming hours`() {
        val list = hours(startHour = 6, count = 3)
        val upcoming = HourlyWindow.upcoming(list, at(23, 0), 24)
        assertTrue(upcoming.isEmpty())
    }

    @Test
    fun `a snapshot fetched just before the hour starts at its first slot`() {
        // Every slot is in the future, so index -1 must not drop the first entry.
        val list = hours(startHour = 19, count = 5)
        assertEquals(-1, HourlyWindow.currentIndex(list, at(18, 55)))
        assertEquals(at(19), HourlyWindow.upcoming(list, at(18, 55), 24).first().timeEpochMillis)
    }

    @Test
    fun `the widget limit is respected and never returns more than asked`() {
        val list = hours(startHour = 18, count = 24)
        assertEquals(4, HourlyWindow.upcoming(list, at(18, 16), 4).size)
        assertEquals(0, HourlyWindow.upcoming(list, at(18, 16), 0).size)
        assertEquals(0, HourlyWindow.upcoming(emptyList(), at(18, 16), 4).size)
    }

    @Test
    fun `consecutive hours stay consecutive after the window slides`() {
        val list = hours(startHour = 18, count = 24)
        val upcoming = HourlyWindow.upcoming(list, at(20, 40), 4)
        upcoming.zipWithNext().forEach { (a, b) ->
            assertEquals(hourMs, b.timeEpochMillis - a.timeEpochMillis)
        }
    }
}
