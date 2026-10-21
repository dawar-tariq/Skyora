package com.skyora.weather.domain

import com.skyora.weather.domain.model.CurrentWeather
import com.skyora.weather.domain.model.HourlyForecast
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.domain.notifications.RainAlertPolicy
import com.skyora.weather.domain.model.DefaultLocations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Rain alerts must be driven by precipitation probability and must not repeat. */
class RainAlertPolicyTest {

    private val now = 1_761_000_000_000L
    private val hourMs = 3_600_000L

    private fun hour(offsetHours: Int, probability: Int?, code: Int = 61) = HourlyForecast(
        timeEpochMillis = now + offsetHours * hourMs,
        temperatureC = 12.0,
        weatherCode = code,
        isDay = true,
        precipitationProbability = probability,
    )

    private fun snapshot(vararg hours: HourlyForecast) = WeatherSnapshot(
        location = DefaultLocations.Srinagar,
        timezone = "Asia/Kolkata",
        utcOffsetSeconds = 19_800,
        current = CurrentWeather(
            timeEpochMillis = now,
            temperatureC = 14.0,
            feelsLikeC = 14.0,
            weatherCode = 1,
            isDay = true,
        ),
        hourly = hours.toList(),
        daily = emptyList(),
        fetchedAtEpochMillis = now,
    )

    @Test
    fun `picks the first hour at or above the threshold`() {
        val match = RainAlertPolicy.firstRainyHour(
            snapshot = snapshot(hour(1, 20), hour(2, 55), hour(3, 80)),
            thresholdPercent = 60,
            lookaheadHours = 6,
            nowMillis = now,
        )
        assertEquals(80, match?.probability)
        assertEquals(now + 3 * hourMs, match?.hour?.timeEpochMillis)
    }

    @Test
    fun `ignores hours outside the lookahead window`() {
        val match = RainAlertPolicy.firstRainyHour(
            snapshot = snapshot(hour(9, 95)),
            thresholdPercent = 60,
            lookaheadHours = 6,
            nowMillis = now,
        )
        assertNull(match)
    }

    @Test
    fun `ignores past hours`() {
        val match = RainAlertPolicy.firstRainyHour(
            snapshot = snapshot(hour(-2, 99), hour(2, 0)),
            thresholdPercent = 60,
            lookaheadHours = 6,
            nowMillis = now,
        )
        assertNull(match)
    }

    @Test
    fun `a rainy code without a probability does not notify`() {
        val match = RainAlertPolicy.firstRainyHour(
            snapshot = snapshot(hour(1, null, code = 95)),
            thresholdPercent = 40,
            lookaheadHours = 6,
            nowMillis = now,
        )
        assertNull(match)
    }

    @Test
    fun `probability alone is enough even for a clear code`() {
        val match = RainAlertPolicy.firstRainyHour(
            snapshot = snapshot(hour(1, 70, code = 1)),
            thresholdPercent = 60,
            lookaheadHours = 6,
            nowMillis = now,
        )
        assertEquals(70, match?.probability)
    }

    @Test
    fun `dedupe key is stable per location and hour`() {
        val s = snapshot(hour(1, 90))
        val first = s.hourly.first()
        assertEquals(
            RainAlertPolicy.eventKey(s, first),
            RainAlertPolicy.eventKey(s, first.copy()),
        )
        assertNotEquals(
            RainAlertPolicy.eventKey(s, first),
            RainAlertPolicy.eventKey(s, first.copy(timeEpochMillis = first.timeEpochMillis + hourMs)),
        )
    }
}
