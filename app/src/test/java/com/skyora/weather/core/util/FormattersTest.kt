package com.skyora.weather.core.util

import com.skyora.weather.domain.model.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * Pure JVM checks for the display formatting layer. No Android classes are
 * involved, so these run on the JVM without Robolectric.
 */
class FormattersTest {

    @Test
    fun `temperature rounds to whole degrees`() {
        assertEquals("12°", formatTemperature(12.4, TemperatureUnit.CELSIUS))
        assertEquals("54°", formatTemperature(12.4, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun `negative zero is displayed as zero`() {
        assertEquals("0°", formatTemperature(-0.4, TemperatureUnit.CELSIUS))
    }

    @Test
    fun `missing temperature uses a placeholder instead of NaN`() {
        assertEquals("—", formatTemperature(null, TemperatureUnit.CELSIUS))
        assertEquals("—", formatTemperature(Double.NaN, TemperatureUnit.CELSIUS))
    }

    @Test
    fun `temperature with unit carries the unit suffix`() {
        assertEquals("20°C", formatTemperatureWithUnit(20.0, TemperatureUnit.CELSIUS))
        assertEquals("68°F", formatTemperatureWithUnit(20.0, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun `wind speed converts to miles per hour`() {
        assertEquals("10 km/h", formatWindSpeed(10.0, TemperatureUnit.CELSIUS))
        assertEquals("6 mph", formatWindSpeed(10.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("—", formatWindSpeed(null, TemperatureUnit.CELSIUS))
    }

    @Test
    fun `compass labels wrap around the circle`() {
        assertEquals("N", windDirectionLabel(0))
        assertEquals("NE", windDirectionLabel(45))
        assertEquals("E", windDirectionLabel(90))
        assertEquals("S", windDirectionLabel(180))
        assertEquals("N", windDirectionLabel(350))
        assertEquals("N", windDirectionLabel(-10))
        assertNull(windDirectionLabel(null))
    }

    @Test
    fun `update age buckets match the banner copy`() {
        val now = 1_760_000_000_000L
        assertEquals(UpdateAge.JustNow, updateAge(now - 30_000L, now))
        assertEquals(UpdateAge.Minutes(5), updateAge(now - 5 * 60_000L, now))
        assertEquals(UpdateAge.Hours(2), updateAge(now - 2 * 3_600_000L, now))
        assertEquals(UpdateAge.Older, updateAge(now - 30 * 3_600_000L, now))
    }

    @Test
    fun `a clock that moved backwards still reads as just now`() {
        val now = 1_760_000_000_000L
        assertEquals(UpdateAge.JustNow, updateAge(now + 60_000L, now))
    }

    @Test
    fun `visibility converts with the unit system`() {
        assertEquals("24 km", formatVisibility(24_000.0, TemperatureUnit.CELSIUS))
        assertEquals("15 mi", formatVisibility(24_000.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("—", formatVisibility(-1.0, TemperatureUnit.CELSIUS))
    }

    @Test
    fun `daylight duration is humanised`() {
        val sunrise = Instant.parse("2026-06-01T00:30:00Z")
        val sunset = Instant.parse("2026-06-01T13:00:00Z")
        assertEquals("12h 30m", formatDaylight(sunrise, sunset))
        assertNull(formatDaylight(null, sunset))
        assertNull(formatDaylight(sunset, sunrise))
    }

    @Test
    fun `percentages handle null and zero`() {
        assertEquals("45%", formatPercentage(45))
        assertEquals("—", formatPercentage(null))
        assertEquals("0%", formatChance(0))
        assertNull(formatChance(null))
    }

    @Test
    fun `dry days do not show a misleading decimal`() {
        assertEquals("0 mm", formatPrecipitation(0.0))
        assertEquals("0 mm", formatPrecipitation(0.01))
    }

    @Test
    fun `uv bands follow the WHO thresholds`() {
        assertEquals(UvCategory.LOW, uvCategory(2.9))
        assertEquals(UvCategory.MODERATE, uvCategory(3.0))
        assertEquals(UvCategory.HIGH, uvCategory(7.0))
        assertEquals(UvCategory.VERY_HIGH, uvCategory(10.0))
        assertEquals(UvCategory.EXTREME, uvCategory(11.0))
        assertNull(uvCategory(null))
    }

    @Test
    fun `unknown timezone falls back to the numeric offset`() {
        assertEquals("Asia/Kolkata", resolveZoneId("Asia/Kolkata", 19_800).id)
        assertEquals(ZoneId.of("+05:30"), resolveZoneId("Not/AZone", 19_800))
    }
}
