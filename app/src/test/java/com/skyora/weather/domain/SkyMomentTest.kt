package com.skyora.weather.domain

import com.skyora.weather.domain.model.SkyCondition
import com.skyora.weather.domain.model.SkyMoments
import com.skyora.weather.domain.model.SkyPhase
import com.skyora.weather.domain.model.SunTimes
import com.skyora.weather.domain.model.WeatherCondition
import com.skyora.weather.domain.model.skyCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Deterministic fixtures for the background acceptance criteria.
 *
 * Every case pins an instant and a sunrise/sunset, so the result cannot depend
 * on when the suite runs or on the machine's timezone.
 */
class SkyMomentTest {

    private val srinagar: ZoneId = ZoneId.of("Asia/Kolkata")
    private val tokyo: ZoneId = ZoneId.of("Asia/Tokyo")
    private val day: LocalDate = LocalDate.of(2026, 10, 21)

    private fun at(date: LocalDate, hour: Int, minute: Int, zone: ZoneId): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    /** Srinagar in late October: sunrise 06:45, sunset 17:50 (IST). */
    private val srinagarSun = mapOf(
        day to SunTimes(
            sunriseEpochMillis = at(day, 6, 45, srinagar),
            sunsetEpochMillis = at(day, 17, 50, srinagar),
        ),
    )

    private fun srinagarAt(hour: Int, minute: Int = 0): Long = at(day, hour, minute, srinagar)

    private fun resolve(
        condition: WeatherCondition,
        millis: Long,
        zone: ZoneId = srinagar,
        sun: Map<LocalDate, SunTimes> = srinagarSun,
    ) = SkyMoments.resolve(condition, millis, zone, sun)

    // --- acceptance case 1 ---------------------------------------------------

    @Test
    fun `Srinagar 1-30 PM clear sky is a bright daytime sky`() {
        val moment = resolve(WeatherCondition.CLEAR, srinagarAt(13, 30))
        assertEquals(SkyPhase.DAY, moment.phase)
        assertEquals(SkyCondition.CLEAR, moment.condition)
        assertFalse("daytime must not be night", moment.isNight)
    }

    // --- acceptance case 2 ---------------------------------------------------

    @Test
    fun `Srinagar 1-30 AM clear sky is a night sky`() {
        val moment = resolve(WeatherCondition.CLEAR, srinagarAt(1, 30))
        assertEquals(SkyPhase.NIGHT, moment.phase)
        assertTrue(moment.isNight)
    }

    // --- acceptance case 3 & 4 ----------------------------------------------

    @Test
    fun `partly cloudy renders as daylight cloud and as night cloud`() {
        val dayMoment = resolve(WeatherCondition.PARTLY_CLOUDY, srinagarAt(13, 30))
        val nightMoment = resolve(WeatherCondition.PARTLY_CLOUDY, srinagarAt(1, 30))

        assertEquals(SkyPhase.DAY, dayMoment.phase)
        assertEquals(SkyCondition.CLOUDY, dayMoment.condition)
        assertEquals(SkyPhase.NIGHT, nightMoment.phase)
        assertEquals(SkyCondition.CLOUDY, nightMoment.condition)
        assertNotEquals(dayMoment, nightMoment)
    }

    // --- acceptance case 5 ---------------------------------------------------

    @Test
    fun `daytime rain stays daylight rather than a midnight scene`() {
        for (condition in listOf(
            WeatherCondition.RAIN,
            WeatherCondition.DRIZZLE,
            WeatherCondition.RAIN_SHOWERS,
            WeatherCondition.THUNDERSTORM,
        )) {
            val moment = resolve(condition, srinagarAt(12, 0))
            assertEquals("$condition", SkyPhase.DAY, moment.phase)
        }
        assertEquals(SkyCondition.RAIN, resolve(WeatherCondition.RAIN, srinagarAt(12)).condition)
        assertEquals(
            SkyCondition.STORM,
            resolve(WeatherCondition.THUNDERSTORM, srinagarAt(12)).condition,
        )
    }

    // --- acceptance case 6 ---------------------------------------------------

    @Test
    fun `sunrise and sunset produce the warm transition phase`() {
        assertEquals("just before sunrise", SkyPhase.DAWN, resolve(WeatherCondition.CLEAR, srinagarAt(6, 20)).phase)
        assertEquals("at sunrise", SkyPhase.DAWN, resolve(WeatherCondition.CLEAR, srinagarAt(6, 45)).phase)
        assertEquals("just after sunrise", SkyPhase.DAWN, resolve(WeatherCondition.CLEAR, srinagarAt(7, 5)).phase)
        assertEquals("sunset approach", SkyPhase.DUSK, resolve(WeatherCondition.CLEAR, srinagarAt(17, 15)).phase)
        assertEquals("at sunset", SkyPhase.DUSK, resolve(WeatherCondition.CLEAR, srinagarAt(17, 50)).phase)
    }

    @Test
    fun `sunset styling does not bleed into the afternoon or the night`() {
        assertEquals("15:00 is still day", SkyPhase.DAY, resolve(WeatherCondition.CLEAR, srinagarAt(15, 0)).phase)
        assertEquals("16:00 is still day", SkyPhase.DAY, resolve(WeatherCondition.CLEAR, srinagarAt(16, 0)).phase)
        assertEquals("19:00 is night", SkyPhase.NIGHT, resolve(WeatherCondition.CLEAR, srinagarAt(19, 0)).phase)
        assertEquals("23:00 is night", SkyPhase.NIGHT, resolve(WeatherCondition.CLEAR, srinagarAt(23, 0)).phase)
        assertEquals("04:00 is night", SkyPhase.NIGHT, resolve(WeatherCondition.CLEAR, srinagarAt(4, 0)).phase)
    }

    // --- acceptance case 7 ---------------------------------------------------

    @Test
    fun `another city follows its own timezone, not the device clock`() {
        // The same instant: 13:30 in Srinagar is 17:00 in Tokyo.
        val instant = srinagarAt(13, 30)
        val tokyoSun = mapOf(
            day to SunTimes(
                sunriseEpochMillis = at(day, 5, 50, tokyo),
                sunsetEpochMillis = at(day, 17, 5, tokyo),
            ),
        )

        assertEquals(SkyPhase.DAY, resolve(WeatherCondition.CLEAR, instant).phase)
        assertEquals(
            "Tokyo has already reached its sunset window",
            SkyPhase.DUSK,
            resolve(WeatherCondition.CLEAR, instant, zone = tokyo, sun = tokyoSun).phase,
        )
    }

    @Test
    fun `a night launch in one zone does not make another zone look like night`() {
        // 02:00 in Tokyo is 22:30 the previous evening in Srinagar; both are
        // night here, so use a case where they differ: 08:00 Tokyo = 04:30 IST.
        val instant = at(day, 8, 0, tokyo)
        val tokyoSun = mapOf(
            day to SunTimes(
                sunriseEpochMillis = at(day, 5, 50, tokyo),
                sunsetEpochMillis = at(day, 17, 5, tokyo),
            ),
        )
        assertEquals(SkyPhase.DAY, resolve(WeatherCondition.CLEAR, instant, tokyo, tokyoSun).phase)
        assertEquals(SkyPhase.NIGHT, resolve(WeatherCondition.CLEAR, instant, srinagar, srinagarSun).phase)
    }

    // --- missing data --------------------------------------------------------

    @Test
    fun `missing sun data falls back to daylight hours instead of night`() {
        val noon = resolve(WeatherCondition.CLEAR, srinagarAt(12, 0), sun = emptyMap())
        val lateNight = resolve(WeatherCondition.CLEAR, srinagarAt(2, 0), sun = emptyMap())

        assertEquals("noon without sun data must not be night", SkyPhase.DAY, noon.phase)
        assertEquals(SkyPhase.NIGHT, lateNight.phase)
    }

    @Test
    fun `missing sun data has no dawn or dusk window`() {
        for (minute in listOf(6 * 60, 6 * 60 + 20, 6 * 60 + 45, 17 * 60 + 30, 18 * 60)) {
            val millis = srinagarAt(minute / 60, minute % 60)
            val phase = resolve(WeatherCondition.CLEAR, millis, sun = emptyMap()).phase
            assertTrue("$minute -> $phase", phase == SkyPhase.DAY || phase == SkyPhase.NIGHT)
        }
    }

    @Test
    fun `only sunrise known still yields a sane phase`() {
        val partial = mapOf(day to SunTimes(at(day, 6, 45, srinagar), null))
        assertEquals(SkyPhase.DAWN, resolve(WeatherCondition.CLEAR, srinagarAt(6, 30), sun = partial).phase)
        assertEquals(SkyPhase.DAY, resolve(WeatherCondition.CLEAR, srinagarAt(11, 0), sun = partial).phase)
        assertEquals(SkyPhase.NIGHT, resolve(WeatherCondition.CLEAR, srinagarAt(23, 0), sun = partial).phase)
    }

    @Test
    fun `a date boundary does not reuse yesterday's sunset window`() {
        val yesterday = day.minusDays(1)
        val sun = mapOf(
            yesterday to SunTimes(at(yesterday, 6, 46, srinagar), at(yesterday, 17, 51, srinagar)),
            day to SunTimes(at(day, 6, 45, srinagar), at(day, 17, 50, srinagar)),
        )
        assertEquals(
            "00:20 today is night, not yesterday's dusk",
            SkyPhase.NIGHT,
            resolve(WeatherCondition.CLEAR, srinagarAt(0, 20), sun = sun).phase,
        )
    }

    // --- condition mapping ---------------------------------------------------

    @Test
    fun `every weather condition maps to a background condition`() {
        val expected = mapOf(
            WeatherCondition.CLEAR to SkyCondition.CLEAR,
            WeatherCondition.MAINLY_CLEAR to SkyCondition.CLEAR,
            WeatherCondition.PARTLY_CLOUDY to SkyCondition.CLOUDY,
            WeatherCondition.OVERCAST to SkyCondition.OVERCAST,
            WeatherCondition.FOG to SkyCondition.FOG,
            WeatherCondition.DRIZZLE to SkyCondition.RAIN,
            WeatherCondition.FREEZING_DRIZZLE to SkyCondition.RAIN,
            WeatherCondition.RAIN to SkyCondition.RAIN,
            WeatherCondition.FREEZING_RAIN to SkyCondition.RAIN,
            WeatherCondition.RAIN_SHOWERS to SkyCondition.RAIN,
            WeatherCondition.SNOW to SkyCondition.SNOW,
            WeatherCondition.SNOW_GRAINS to SkyCondition.SNOW,
            WeatherCondition.SNOW_SHOWERS to SkyCondition.SNOW,
            WeatherCondition.THUNDERSTORM to SkyCondition.STORM,
            WeatherCondition.THUNDERSTORM_HAIL to SkyCondition.STORM,
            WeatherCondition.UNKNOWN to SkyCondition.CLOUDY,
        )
        expected.forEach { (weather, sky) ->
            assertEquals(weather.name, sky, weather.skyCondition())
        }
        assertEquals("no condition may be unmapped", 16, WeatherCondition.entries.size)
    }

    // --- clock-only fallback -------------------------------------------------

    @Test
    fun `the no-data fallback follows the clock and never assumes night`() {
        assertEquals(SkyPhase.DAY, SkyMoments.phaseForLocalClock(srinagarAt(13, 30), srinagar))
        assertEquals(SkyPhase.NIGHT, SkyMoments.phaseForLocalClock(srinagarAt(1, 30), srinagar))
        assertEquals(SkyPhase.DAY, SkyMoments.phaseForLocalClock(srinagarAt(6, 0), srinagar))
        assertEquals(SkyPhase.NIGHT, SkyMoments.phaseForLocalClock(srinagarAt(18, 0), srinagar))
    }
}
