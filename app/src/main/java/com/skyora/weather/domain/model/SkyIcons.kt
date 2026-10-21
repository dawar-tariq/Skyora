package com.skyora.weather.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Which artwork an item should use.
 *
 * [DAY], [NIGHT] and [NEUTRAL] only matter for conditions whose icon changes
 * between day and night (clear / mainly clear / partly cloudy). Precipitation,
 * fog and storms look the same at 03:00 and 15:00, which also keeps the hourly
 * strip readable at a glance.
 */
enum class IconDayPart { DAY, NIGHT, NEUTRAL }

/**
 * The single source of truth for "which sky is it right now".
 *
 * Day/night is derived from each item's **own** timestamp, the location's
 * timezone and that calendar day's sunrise/sunset — never from the device
 * clock, which is a common source of moons appearing in a midday forecast.
 *
 * Everything (Compose, widgets, notifications) resolves its artwork through
 * [iconKey] so the same forecast can never show two different icons.
 */
object SkyIcons {

    /** Fallback when a payload has no sunrise/sunset at all. */
    private const val FALLBACK_DAY_START_HOUR = 6
    private const val FALLBACK_DAY_END_HOUR = 18

    /**
     * `true` when [epochMillis] falls between sunrise and sunset of the same
     * calendar day in [zone].
     *
     * Behaviour with partial data:
     *  - both times known for that date -> exact comparison;
     *  - only sunrise known -> sunrise…soonest of (sunset, next 12 h);
     *  - only sunset known -> (sunset − 12 h)…sunset;
     *  - neither known -> 06:00–18:00 local, so the UI degrades to daylight
     *    hours instead of showing a moon at noon.
     *
     * [sunTimes] must already be keyed by local calendar date.
     */
    fun isDaylight(
        epochMillis: Long,
        zone: ZoneId,
        sunTimes: Map<LocalDate, SunTimes>,
    ): Boolean {
        val instant = Instant.ofEpochMilli(epochMillis)
        val localDate = instant.atZone(zone).toLocalDate()

        // 1. Exact windows. Polar days/nights can make one calendar day's window
        //    spill into the next, so the neighbouring dates are checked too.
        for (date in listOf(localDate.minusDays(1), localDate, localDate.plusDays(1))) {
            val window = sunTimes[date] ?: continue
            val sunrise = window.sunriseEpochMillis
            val sunset = window.sunsetEpochMillis
            if (sunrise != null && sunset != null && sunset > sunrise &&
                epochMillis >= sunrise && epochMillis < sunset
            ) {
                return true
            }
        }

        // 2. A complete window existed for this date but did not match -> night.
        val today = sunTimes[localDate]
        if (today?.sunriseEpochMillis != null && today.sunsetEpochMillis != null &&
            today.sunsetEpochMillis > today.sunriseEpochMillis
        ) {
            return false
        }

        // 3. Partial data: infer the missing edge instead of falling back to fixed hours.
        if (today != null) {
            val sunrise = today.sunriseEpochMillis
            val sunset = today.sunsetEpochMillis
            if (sunrise != null) return epochMillis in sunrise..(sunrise + DAY_MS_APPROX)
            if (sunset != null) return epochMillis in (sunset - DAY_MS_APPROX)..sunset
        }

        // 4. No sun data at all: 06:00-18:00 local beats showing a moon at noon.
        val hour = instant.atZone(zone).hour
        return hour in FALLBACK_DAY_START_HOUR until FALLBACK_DAY_END_HOUR
    }

    /** Day part for artwork selection. */
    fun dayPart(
        condition: WeatherCondition,
        epochMillis: Long,
        zone: ZoneId,
        sunTimes: Map<LocalDate, SunTimes>,
    ): IconDayPart = when (condition) {
        WeatherCondition.CLEAR,
        WeatherCondition.MAINLY_CLEAR,
        WeatherCondition.PARTLY_CLOUDY,
        WeatherCondition.RAIN_SHOWERS,
        -> if (isDaylight(epochMillis, zone, sunTimes)) IconDayPart.DAY else IconDayPart.NIGHT

        else -> IconDayPart.NEUTRAL
    }

    /**
     * Stable key for an icon: `weather_clear_day`, `weather_rain`, ...
     * Matches the generated vector drawables one-for-one.
     */
    fun iconKey(
        condition: WeatherCondition,
        epochMillis: Long,
        zone: ZoneId,
        sunTimes: Map<LocalDate, SunTimes>,
    ): String = when (val part = dayPart(condition, epochMillis, zone, sunTimes)) {
        IconDayPart.NEUTRAL -> "weather_${condition.slug()}"
        IconDayPart.DAY -> "weather_${condition.slug()}_day"
        IconDayPart.NIGHT -> "weather_${condition.slug()}_night"
    }

    /** Convenience for a snapshot's current conditions. */
    fun currentIconKey(snapshot: WeatherSnapshot): String = iconKey(
        condition = snapshot.currentCondition(),
        epochMillis = snapshot.current.timeEpochMillis,
        zone = snapshot.zoneId(),
        sunTimes = snapshot.sunTimes(),
    )

    private fun WeatherCondition.slug(): String = when (this) {
        WeatherCondition.CLEAR -> "clear"
        WeatherCondition.MAINLY_CLEAR -> "mainly_clear"
        WeatherCondition.PARTLY_CLOUDY -> "partly_cloudy"
        WeatherCondition.OVERCAST -> "overcast"
        WeatherCondition.FOG -> "fog"
        WeatherCondition.DRIZZLE -> "drizzle"
        WeatherCondition.FREEZING_DRIZZLE -> "freezing_drizzle"
        WeatherCondition.RAIN -> "rain"
        WeatherCondition.FREEZING_RAIN -> "freezing_rain"
        WeatherCondition.SNOW -> "snow"
        WeatherCondition.SNOW_GRAINS -> "snow_grains"
        WeatherCondition.RAIN_SHOWERS -> "rain_showers"
        WeatherCondition.SNOW_SHOWERS -> "snow_showers"
        WeatherCondition.THUNDERSTORM -> "thunderstorm"
        WeatherCondition.THUNDERSTORM_HAIL -> "thunderstorm_hail"
        WeatherCondition.UNKNOWN -> "unknown"
    }

    private const val DAY_MS_APPROX = 12L * 60L * 60L * 1000L
}

/** Sun times for one local calendar day at one location. */
data class SunTimes(
    val sunriseEpochMillis: Long?,
    val sunsetEpochMillis: Long?,
) {
    val hasData: Boolean get() = sunriseEpochMillis != null || sunsetEpochMillis != null
}

/** Builds the date-keyed sunrise/sunset lookup used by [SkyIcons]. */
fun WeatherSnapshot.sunTimes(): Map<LocalDate, SunTimes> = daily
    .mapNotNull { day -> day.localDate()?.let { it to SunTimes(day.sunriseEpochMillis, day.sunsetEpochMillis) } }
    .toMap()
