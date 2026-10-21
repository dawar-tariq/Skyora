package com.skyora.weather.domain.model

import kotlinx.serialization.Serializable

/** Coarse condition families used for icons, backgrounds and copy. */
@Serializable
enum class WeatherCondition {
    CLEAR,
    MAINLY_CLEAR,
    PARTLY_CLOUDY,
    OVERCAST,
    FOG,
    DRIZZLE,
    FREEZING_DRIZZLE,
    RAIN,
    FREEZING_RAIN,
    SNOW,
    SNOW_GRAINS,
    RAIN_SHOWERS,
    SNOW_SHOWERS,
    THUNDERSTORM,
    THUNDERSTORM_HAIL,
    UNKNOWN,
    ;

    val isPrecipitation: Boolean
        get() = this in setOf(
            DRIZZLE, FREEZING_DRIZZLE, RAIN, FREEZING_RAIN, SNOW, SNOW_GRAINS,
            RAIN_SHOWERS, SNOW_SHOWERS, THUNDERSTORM, THUNDERSTORM_HAIL,
        )

    val isSnow: Boolean
        get() = this in setOf(SNOW, SNOW_GRAINS, SNOW_SHOWERS)

    val isStorm: Boolean
        get() = this in setOf(THUNDERSTORM, THUNDERSTORM_HAIL)
}

/**
 * WMO weather interpretation codes returned by Open-Meteo.
 * https://open-meteo.com/en/docs (Weather variable documentation)
 */
object WmoWeatherCodes {
    fun conditionFor(code: Int): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1 -> WeatherCondition.MAINLY_CLEAR
        2 -> WeatherCondition.PARTLY_CLOUDY
        3 -> WeatherCondition.OVERCAST
        45, 48 -> WeatherCondition.FOG
        51, 53, 55 -> WeatherCondition.DRIZZLE
        56, 57 -> WeatherCondition.FREEZING_DRIZZLE
        61, 63, 65 -> WeatherCondition.RAIN
        66, 67 -> WeatherCondition.FREEZING_RAIN
        71, 73, 75 -> WeatherCondition.SNOW
        77 -> WeatherCondition.SNOW_GRAINS
        80, 81, 82 -> WeatherCondition.RAIN_SHOWERS
        85, 86 -> WeatherCondition.SNOW_SHOWERS
        95 -> WeatherCondition.THUNDERSTORM
        96, 99 -> WeatherCondition.THUNDERSTORM_HAIL
        else -> WeatherCondition.UNKNOWN
    }
}
