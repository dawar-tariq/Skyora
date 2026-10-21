package com.skyora.weather.data

import com.skyora.weather.data.mapper.ForecastMapper
import com.skyora.weather.data.remote.dto.ForecastResponseDto
import com.skyora.weather.domain.model.DefaultLocations
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Parses a real-shaped Open-Meteo payload and asserts the mapper keeps the
 * API's local timezone, drops implausible data and rejects a truncated body.
 * The JSON deliberately uses the API's snake_case field names so a wrong
 * @SerialName in the DTOs fails here rather than on a user's phone.
 */
class ForecastParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val location = DefaultLocations.Srinagar

    private val fetchedAt = Instant.parse("2026-10-20T04:30:00Z").toEpochMilli()

    private val payload = """
        {
          "latitude": 34.08,
          "longitude": 74.79,
          "elevation": 1585.0,
          "utc_offset_seconds": 19800,
          "timezone": "Asia/Kolkata",
          "timezone_abbreviation": "IST",
          "current": {
            "time": "2026-10-20T10:00",
            "interval": 900,
            "temperature_2m": 18.4,
            "relative_humidity_2m": 62,
            "apparent_temperature": 17.9,
            "is_day": 1,
            "precipitation": 0.0,
            "weather_code": 1,
            "cloud_cover": 22,
            "pressure_msl": 1014.2,
            "wind_speed_10m": 9.7,
            "wind_direction_10m": 210,
            "wind_gusts_10m": 18.2
          },
          "hourly": {
            "time": ["2026-10-20T09:00", "2026-10-20T10:00", "2026-10-20T11:00"],
            "temperature_2m": [17.2, 18.4, 19.6],
            "apparent_temperature": [16.9, 17.9, 19.1],
            "relative_humidity_2m": [68, 62, 58],
            "weather_code": [1, 1, 2],
            "precipitation_probability": [0, 5, 10],
            "is_day": [1, 1, 1],
            "visibility": [20000.0, 24000.0, 25000.0]
          },
          "daily": {
            "time": ["2026-10-20", "2026-10-21"],
            "weather_code": [1, 3],
            "temperature_2m_max": [22.1, 21.0],
            "temperature_2m_min": [9.4, 10.1],
            "sunrise": ["2026-10-20T06:42", "2026-10-21T06:43"],
            "sunset": ["2026-10-20T17:52", "2026-10-21T17:51"],
            "uv_index_max": [5.1, 4.2],
            "precipitation_probability_max": [10, 25],
            "wind_speed_10m_max": [14.0, 12.0]
          }
        }
    """.trimIndent()

    @Test
    fun `payload maps to a complete snapshot`() {
        val dto = json.decodeFromString<ForecastResponseDto>(payload)
        val snapshot = ForecastMapper().map(dto, location, fetchedAt)

        assertNotNull(snapshot)
        snapshot!!
        assertEquals("Asia/Kolkata", snapshot.timezone)
        assertEquals(19_800, snapshot.utcOffsetSeconds)
        assertEquals(location, snapshot.location)
        assertEquals(fetchedAt, snapshot.fetchedAtEpochMillis)
        assertEquals(18.4, snapshot.current.temperatureC, 0.0001)
        assertEquals(17.9, snapshot.current.feelsLikeC, 0.0001)
        assertEquals(true, snapshot.current.isDay)
        assertEquals(62, snapshot.current.humidityPercent)
        assertEquals(1, snapshot.current.weatherCode)
        assertEquals(2, snapshot.daily.size)
    }

    @Test
    fun `current visibility comes from the matching past hour`() {
        val dto = json.decodeFromString<ForecastResponseDto>(payload)
        val snapshot = ForecastMapper().map(dto, location, fetchedAt)

        // 10:00 IST == 04:30Z, the middle hourly slot.
        assertEquals(24_000.0, snapshot?.current?.visibilityMeters ?: -1.0, 0.0001)
    }

    @Test
    fun `upcoming hours are ordered and limited`() {
        val dto = json.decodeFromString<ForecastResponseDto>(payload)
        val snapshot = ForecastMapper().map(dto, location, fetchedAt)!!
        val times = snapshot.hourly.map { it.timeEpochMillis }

        assertEquals(3, times.size)
        assertEquals(times.sorted(), times)
        assertTrue(times.all { it >= fetchedAt - 3_600_000L })
    }

    @Test
    fun `daily extremes and sun times survive mapping`() {
        val dto = json.decodeFromString<ForecastResponseDto>(payload)
        val snapshot = ForecastMapper().map(dto, location, fetchedAt)!!
        val today = snapshot.daily.first()

        assertEquals("2026-10-20", today.dateIso)
        assertEquals(22.1, today.tempMaxC, 0.0001)
        assertEquals(9.4, today.tempMinC, 0.0001)
        assertNotNull(today.sunrise())
        assertNotNull(today.sunset())
        assertTrue(today.sunrise()!!.isBefore(today.sunset()!!))
    }

    @Test
    fun `a response without current conditions is rejected`() {
        val truncated = """{"hourly":{"time":[]},"daily":{"time":[]}}"""
        val dto = json.decodeFromString<ForecastResponseDto>(truncated)

        assertNull(ForecastMapper().map(dto, location, fetchedAt))
    }

    @Test
    fun `a response without daily data is rejected`() {
        val noDaily = """
            {
              "timezone": "UTC",
              "utc_offset_seconds": 0,
              "current": {"time": "2026-10-20T04:30", "temperature_2m": 12.0, "weather_code": 3},
              "daily": {"time": []}
            }
        """.trimIndent()
        val dto = json.decodeFromString<ForecastResponseDto>(noDaily)

        assertNull(ForecastMapper().map(dto, location, fetchedAt))
    }

    @Test
    fun `an implausible temperature is rejected instead of shown`() {
        val broken = """
            {
              "timezone": "UTC",
              "utc_offset_seconds": 0,
              "current": {"time": "2026-10-20T04:30", "temperature_2m": 900.0},
              "daily": {"time": ["2026-10-20"], "temperature_2m_max": [1.0], "temperature_2m_min": [0.0]}
            }
        """.trimIndent()
        val dto = json.decodeFromString<ForecastResponseDto>(broken)

        assertNull(ForecastMapper().map(dto, location, fetchedAt))
    }
}
