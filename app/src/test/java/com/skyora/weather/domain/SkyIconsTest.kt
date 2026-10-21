package com.skyora.weather.domain

import com.skyora.weather.domain.model.IconDayPart
import com.skyora.weather.domain.model.SkyIcons
import com.skyora.weather.domain.model.SunTimes
import com.skyora.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Regression tests for the "moon at noon" bug.
 *
 * Day/night must be derived from each forecast hour's own timestamp in the
 * location's timezone, against that day's sunrise/sunset — not from the device
 * clock or a payload-wide is_day flag.
 */
class SkyIconsTest {

    private val kolkata: ZoneId = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 10, 20)

    private fun millis(date: LocalDate, hour: Int, minute: Int = 0, zone: ZoneId = kolkata): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    private val sunTimes = mapOf(
        day to SunTimes(
            sunriseEpochMillis = millis(day, 6, 42),
            sunsetEpochMillis = millis(day, 17, 52),
        ),
        day.plusDays(1) to SunTimes(
            sunriseEpochMillis = millis(day.plusDays(1), 6, 43),
            sunsetEpochMillis = millis(day.plusDays(1), 17, 51),
        ),
    )

    @Test
    fun `midday hours are daytime`() {
        for (hour in 7..17) {
            assertTrue("hour $hour", SkyIcons.isDaylight(millis(day, hour), kolkata, sunTimes))
        }
    }

    @Test
    fun `pre-dawn and evening hours are night`() {
        assertFalse(SkyIcons.isDaylight(millis(day, 3), kolkata, sunTimes))
        assertFalse(SkyIcons.isDaylight(millis(day, 6, 0), kolkata, sunTimes))
        assertFalse(SkyIcons.isDaylight(millis(day, 20), kolkata, sunTimes))
        assertFalse(SkyIcons.isDaylight(millis(day, 23, 30), kolkata, sunTimes))
    }

    @Test
    fun `the whole hourly strip never shows a moon during daylight`() {
        val codes = listOf(0, 1, 2)
        for (hour in 8..17) {
            val instant = millis(day, hour)
            codes.forEach { code ->
                val condition = com.skyora.weather.domain.model.WmoWeatherCodes.conditionFor(code)
                assertEquals(
                    "hour $hour code $code",
                    IconDayPart.DAY,
                    SkyIcons.dayPart(condition, instant, kolkata, sunTimes),
                )
            }
        }
    }

    @Test
    fun `after sunset the same condition switches to night artwork`() {
        val condition = WeatherCondition.CLEAR
        assertEquals(
            IconDayPart.DAY,
            SkyIcons.dayPart(condition, millis(day, 15), kolkata, sunTimes),
        )
        assertEquals(
            IconDayPart.NIGHT,
            SkyIcons.dayPart(condition, millis(day, 22), kolkata, sunTimes),
        )
        assertEquals("weather_clear_day", SkyIcons.iconKey(condition, millis(day, 12), kolkata, sunTimes))
        assertEquals("weather_clear_night", SkyIcons.iconKey(condition, millis(day, 23), kolkata, sunTimes))
    }

    @Test
    fun `rain and storms keep one icon regardless of hour`() {
        val rain = WeatherCondition.RAIN
        assertEquals(
            SkyIcons.iconKey(rain, millis(day, 12), kolkata, sunTimes),
            SkyIcons.iconKey(rain, millis(day, 23), kolkata, sunTimes),
        )
        assertEquals("weather_rain", SkyIcons.iconKey(rain, millis(day, 12), kolkata, sunTimes))
    }

    @Test
    fun `timezone is respected, not the device clock`() {
        // 08:00 in Tokyo is 04:30 in Kolkata: day in Tokyo, still night in Kolkata.
        val tokyo = ZoneId.of("Asia/Tokyo")
        val tokyoDay = mapOf(
            day to SunTimes(millis(day, 5, 50, tokyo), millis(day, 17, 5, tokyo)),
        )
        val instant = millis(day, 8, 0, tokyo)
        assertTrue(SkyIcons.isDaylight(instant, tokyo, tokyoDay))
        val kolkataTimes = mapOf(
            day to SunTimes(millis(day, 6, 42), millis(day, 17, 52)),
        )
        assertFalse(SkyIcons.isDaylight(instant, kolkata, kolkataTimes))
    }

    @Test
    fun `missing sun data falls back to daylight hours instead of showing a moon`() {
        val noon = millis(day, 12)
        assertTrue(SkyIcons.isDaylight(noon, kolkata, emptyMap()))
        assertFalse(SkyIcons.isDaylight(millis(day, 2), kolkata, emptyMap()))
        assertEquals(
            "weather_partly_cloudy_day",
            SkyIcons.iconKey(WeatherCondition.PARTLY_CLOUDY, noon, kolkata, emptyMap()),
        )
    }

    @Test
    fun `only sunrise known still infers a plausible day window`() {
        val partial = mapOf(day to SunTimes(millis(day, 6, 30), null))
        assertTrue(SkyIcons.isDaylight(millis(day, 9), kolkata, partial))
        assertFalse(SkyIcons.isDaylight(millis(day, 2), kolkata, partial))
    }

    @Test
    fun `only sunset known still infers a plausible day window`() {
        val partial = mapOf(day to SunTimes(null, millis(day, 18, 0)))
        assertTrue(SkyIcons.isDaylight(millis(day, 14), kolkata, partial))
        assertFalse(SkyIcons.isDaylight(millis(day, 22), kolkata, partial))
    }

    @Test
    fun `a date boundary does not leak yesterday's window into today`() {
        val yesterday = day.minusDays(1)
        val times = mapOf(
            yesterday to SunTimes(millis(yesterday, 6, 40), millis(yesterday, 17, 53)),
            day to SunTimes(millis(day, 6, 42), millis(day, 17, 52)),
        )
        // 00:30 today is night even though yesterday had a sunset 6.5 h earlier.
        assertFalse(SkyIcons.isDaylight(millis(day, 0, 30), kolkata, times))
    }

    @Test
    fun `unknown weather codes still get a safe icon`() {
        val key = SkyIcons.iconKey(WeatherCondition.UNKNOWN, millis(day, 12), kolkata, sunTimes)
        assertEquals("weather_unknown", key)
    }
}
