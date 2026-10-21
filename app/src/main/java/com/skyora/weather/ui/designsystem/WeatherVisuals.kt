package com.skyora.weather.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.skyora.weather.R
import com.skyora.weather.core.util.UvCategory
import com.skyora.weather.domain.model.SkyIcons
import com.skyora.weather.domain.model.WeatherCondition
import com.skyora.weather.domain.model.WmoWeatherCodes
import com.skyora.weather.domain.model.sunTimes

enum class WeatherVisualFamily { CLEAR, CLOUDY, RAIN, SNOW, STORM, FOG }

fun WeatherCondition.visualFamily(): WeatherVisualFamily = when (this) {
    WeatherCondition.CLEAR, WeatherCondition.MAINLY_CLEAR -> WeatherVisualFamily.CLEAR
    WeatherCondition.PARTLY_CLOUDY, WeatherCondition.OVERCAST, WeatherCondition.UNKNOWN ->
        WeatherVisualFamily.CLOUDY
    WeatherCondition.FOG -> WeatherVisualFamily.FOG
    WeatherCondition.DRIZZLE, WeatherCondition.FREEZING_DRIZZLE, WeatherCondition.RAIN,
    WeatherCondition.FREEZING_RAIN, WeatherCondition.RAIN_SHOWERS -> WeatherVisualFamily.RAIN
    WeatherCondition.SNOW, WeatherCondition.SNOW_GRAINS, WeatherCondition.SNOW_SHOWERS ->
        WeatherVisualFamily.SNOW
    WeatherCondition.THUNDERSTORM, WeatherCondition.THUNDERSTORM_HAIL -> WeatherVisualFamily.STORM
}

/** Precise WMO description, including intensity where the code provides it. */
@StringRes
fun weatherDescriptionRes(code: Int): Int = when (code) {
    0 -> R.string.weather_clear
    1 -> R.string.weather_mainly_clear
    2 -> R.string.weather_partly_cloudy
    3 -> R.string.weather_overcast
    45 -> R.string.weather_fog
    48 -> R.string.weather_depositing_rime_fog
    51 -> R.string.weather_drizzle_light
    53 -> R.string.weather_drizzle_moderate
    55 -> R.string.weather_drizzle_dense
    56 -> R.string.weather_freezing_drizzle_light
    57 -> R.string.weather_freezing_drizzle_dense
    61 -> R.string.weather_rain_slight
    63 -> R.string.weather_rain_moderate
    65 -> R.string.weather_rain_heavy
    66 -> R.string.weather_freezing_rain_light
    67 -> R.string.weather_freezing_rain_heavy
    71 -> R.string.weather_snow_slight
    73 -> R.string.weather_snow_moderate
    75 -> R.string.weather_snow_heavy
    77 -> R.string.weather_snow_grains
    80 -> R.string.weather_rain_showers_slight
    81 -> R.string.weather_rain_showers_moderate
    82 -> R.string.weather_rain_showers_violent
    85 -> R.string.weather_snow_showers_slight
    86 -> R.string.weather_snow_showers_heavy
    95 -> R.string.weather_thunderstorm
    96, 99 -> R.string.weather_thunderstorm_hail
    else -> R.string.weather_unknown
}

@StringRes
fun WeatherCondition.labelRes(): Int = when (this) {
    WeatherCondition.CLEAR -> R.string.weather_clear
    WeatherCondition.MAINLY_CLEAR -> R.string.weather_mainly_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
    WeatherCondition.OVERCAST -> R.string.weather_overcast
    WeatherCondition.FOG -> R.string.weather_fog
    WeatherCondition.DRIZZLE -> R.string.weather_drizzle_moderate
    WeatherCondition.FREEZING_DRIZZLE -> R.string.weather_freezing_drizzle_light
    WeatherCondition.RAIN -> R.string.weather_rain_moderate
    WeatherCondition.FREEZING_RAIN -> R.string.weather_freezing_rain_light
    WeatherCondition.SNOW -> R.string.weather_snow_moderate
    WeatherCondition.SNOW_GRAINS -> R.string.weather_snow_grains
    WeatherCondition.RAIN_SHOWERS -> R.string.weather_rain_showers_moderate
    WeatherCondition.SNOW_SHOWERS -> R.string.weather_snow_showers_heavy
    WeatherCondition.THUNDERSTORM -> R.string.weather_thunderstorm
    WeatherCondition.THUNDERSTORM_HAIL -> R.string.weather_thunderstorm_hail
    WeatherCondition.UNKNOWN -> R.string.weather_unknown
}

@StringRes
fun uvCategoryLabelRes(category: UvCategory?): Int = when (category) {
    UvCategory.LOW -> R.string.uv_low
    UvCategory.MODERATE -> R.string.uv_moderate
    UvCategory.HIGH -> R.string.uv_high
    UvCategory.VERY_HIGH -> R.string.uv_very_high
    UvCategory.EXTREME -> R.string.uv_extreme
    null -> R.string.value_unavailable
}

/**
 * Drawable resource for a condition + day part, mapping the pure
 * [com.skyora.weather.domain.model.SkyIcons] key onto `res/drawable/weather_*`.
 *
 * This is the only place the app converts an icon key into an Android
 * resource, so Compose, widgets and notifications stay in lockstep.
 */
@DrawableRes
fun weatherIconRes(
    condition: WeatherCondition,
    epochMillis: Long,
    zone: java.time.ZoneId,
    sunTimes: Map<java.time.LocalDate, com.skyora.weather.domain.model.SunTimes>,
): Int = when (SkyIcons.iconKey(condition, epochMillis, zone, sunTimes)) {
    "weather_clear_day" -> R.drawable.weather_clear_day
    "weather_clear_night" -> R.drawable.weather_clear_night
    "weather_mainly_clear_day" -> R.drawable.weather_mainly_clear_day
    "weather_mainly_clear_night" -> R.drawable.weather_mainly_clear_night
    "weather_partly_cloudy_day" -> R.drawable.weather_partly_cloudy_day
    "weather_partly_cloudy_night" -> R.drawable.weather_partly_cloudy_night
    "weather_rain_showers_day" -> R.drawable.weather_rain_showers_day
    "weather_rain_showers_night" -> R.drawable.weather_rain_showers_night
    "weather_overcast" -> R.drawable.weather_overcast
    "weather_fog" -> R.drawable.weather_fog
    "weather_drizzle" -> R.drawable.weather_drizzle
    "weather_freezing_drizzle" -> R.drawable.weather_freezing_drizzle
    "weather_rain" -> R.drawable.weather_rain
    "weather_freezing_rain" -> R.drawable.weather_freezing_rain
    "weather_snow" -> R.drawable.weather_snow
    "weather_snow_grains" -> R.drawable.weather_snow_grains
    "weather_snow_showers" -> R.drawable.weather_snow_showers
    "weather_thunderstorm" -> R.drawable.weather_thunderstorm
    "weather_thunderstorm_hail" -> R.drawable.weather_thunderstorm_hail
    else -> R.drawable.weather_unknown
}

/** Icon for a specific forecast hour. */
@DrawableRes
fun weatherIconRes(snapshot: com.skyora.weather.domain.model.WeatherSnapshot, hour: com.skyora.weather.domain.model.HourlyForecast): Int =
    weatherIconRes(
        condition = WmoWeatherCodes.conditionFor(hour.weatherCode),
        epochMillis = hour.timeEpochMillis,
        zone = snapshot.zoneId(),
        sunTimes = snapshot.sunTimes(),
    )

/** Icon for one forecast day: always the daytime artwork. */
@DrawableRes
fun weatherIconRes(day: com.skyora.weather.domain.model.DailyForecast): Int = when (
    WmoWeatherCodes.conditionFor(day.weatherCode)
) {
    WeatherCondition.CLEAR -> R.drawable.weather_clear_day
    WeatherCondition.MAINLY_CLEAR -> R.drawable.weather_mainly_clear_day
    WeatherCondition.PARTLY_CLOUDY -> R.drawable.weather_partly_cloudy_day
    WeatherCondition.RAIN_SHOWERS -> R.drawable.weather_rain_showers_day
    else -> weatherIconRes(WmoWeatherCodes.conditionFor(day.weatherCode))
}

/** Icon for the current conditions of a snapshot. */
@DrawableRes
fun weatherIconRes(snapshot: com.skyora.weather.domain.model.WeatherSnapshot): Int = weatherIconRes(
    condition = snapshot.currentCondition(),
    epochMillis = snapshot.current.timeEpochMillis,
    zone = snapshot.zoneId(),
    sunTimes = snapshot.sunTimes(),
)

/**
 * Icon for a weather code with no time context (e.g. a widget placeholder):
 * resolves to the daytime variant where one exists.
 */
@DrawableRes
fun weatherIconRes(condition: WeatherCondition): Int = when (condition) {
    WeatherCondition.CLEAR -> R.drawable.weather_clear_day
    WeatherCondition.MAINLY_CLEAR -> R.drawable.weather_mainly_clear_day
    WeatherCondition.PARTLY_CLOUDY -> R.drawable.weather_partly_cloudy_day
    WeatherCondition.RAIN_SHOWERS -> R.drawable.weather_rain_showers_day
    else -> weatherIconRes(condition, 0L, java.time.ZoneOffset.UTC, emptyMap())
}
