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

    /**
     * The delivery time follows the *city's* clock. 07:30 in Tokyo is a very
     * different instant than 07:30 in Srinagar, and a user who saved Tokyo must
     * get the summary at Tokyo breakfast time.
     */
    @Test
    fun `delivery time is resolved in the requested city zone`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        // 06:00 in Tokyo == 02:30 in Srinagar on the same date.
        val nowInTokyo = ZonedDateTime.of(2026, 10, 20, 6, 0, 0, 0, tokyo)
        assertEquals(90 * 60_000L, NotificationScheduler.delayUntil(minutes, nowInTokyo))

        val nowInSrinagar = nowInTokyo.withZoneSameInstant(zone)
        assertEquals(5 * hourMs, NotificationScheduler.delayUntil(minutes, nowInSrinagar))
    }

    @Test
    fun `a known city timezone is used for scheduling`() {
        assertEquals(
            ZoneId.of("Asia/Tokyo"),
            NotificationScheduler.zoneFor(
                com.skyora.weather.domain.model.Location(
                    id = "tokyo",
                    name = "Tokyo",
                    latitude = 35.68,
                    longitude = 139.69,
                    timezone = "Asia/Tokyo",
                ),
            ),
        )
    }

    @Test
    fun `an unknown or malformed city timezone falls back to null`() {
        assertEquals(null, NotificationScheduler.zoneFor(null))
        assertEquals(
            null,
            NotificationScheduler.zoneFor(
                com.skyora.weather.domain.model.Location(
                    id = "x",
                    name = "X",
                    latitude = 0.0,
                    longitude = 0.0,
                    timezone = "GMT+5:30",
                ),
            ),
        )
    }

    @Test
    fun `delay is never negative`() {
        for (minute in 0 until 1440 step 5) {
            val delay = NotificationScheduler.delayUntil(minute, at(12, 0))
            assertEquals(true, delay in 0..24 * hourMs)
        }
    }
}
