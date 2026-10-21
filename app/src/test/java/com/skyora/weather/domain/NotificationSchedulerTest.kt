package com.skyora.weather.domain

import com.skyora.weather.notifications.NotificationScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The daily summary must land on the user's chosen wall-clock time and roll
 * over to tomorrow when that time has already passed today.
 */
class NotificationSchedulerTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")
    private val minutes = 7 * 60 + 30 // 07:30

    private fun at(hour: Int, minute: Int, second: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 10, 20, hour, minute, second, 0, zone)

    private val hourMs = 3_600_000L

    @Test
    fun `scheduled later today waits until that time`() {
        assertEquals(90 * 60_000L, NotificationScheduler.delayUntil(minutes, at(6, 0)))
    }

    @Test
    fun `a passed delivery time rolls over to tomorrow`() {
        assertEquals(23 * hourMs + 30 * 60_000L, NotificationScheduler.delayUntil(minutes, at(8, 0)))
    }

    @Test
    fun `the exact delivery minute waits a full day`() {
        assertEquals(24 * hourMs, NotificationScheduler.delayUntil(minutes, at(7, 30)))
    }

    @Test
    fun `midnight delivery is a full day away just after midnight`() {
        assertEquals(24 * hourMs - 60_000L, NotificationScheduler.delayUntil(0, at(0, 1)))
    }

    @Test
    fun `late evening delivery works near the end of the day`() {
        assertEquals(31 * 60_000L, NotificationScheduler.delayUntil(23 * 60 + 59, at(23, 28)))
    }

    @Test
    fun `delay is never negative`() {
        for (minute in 0 until 1440 step 5) {
            val delay = NotificationScheduler.delayUntil(minute, at(12, 0))
            assertEquals(true, delay in 0..24 * hourMs)
        }
    }
}
