package com.skyora.weather.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open-Meteo forecast response.
 * All fields are optional so an incomplete payload can be rejected by the
 * mapper instead of crashing deserialization.
 */
@Serializable
data class ForecastResponseDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val timezone: String? = null,
    @SerialName("timezone_abbreviation") val timezoneAbbreviation: String? = null,
    val current: CurrentWeatherDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null,
)

@Serializable
data class CurrentWeatherDto(
    val time: String? = null,
    val interval: Int? = null,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity: Int? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("cloud_cover") val cloudCover: Int? = null,
    @SerialName("pressure_msl") val pressureMsl: Double? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Int? = null,
    @SerialName("wind_gusts_10m") val windGusts: Double? = null,
)

@Serializable
data class HourlyDto(
    val time: List<String>? = null,
    @SerialName("temperature_2m") val temperature: List<Double?>? = null,
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?>? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity: List<Int?>? = null,
    @SerialName("weather_code") val weatherCode: List<Int?>? = null,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?>? = null,
    @SerialName("is_day") val isDay: List<Int?>? = null,
    val visibility: List<Double?>? = null,
)

@Serializable
data class DailyDto(
    val time: List<String>? = null,
    @SerialName("weather_code") val weatherCode: List<Int?>? = null,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?>? = null,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?>? = null,
    val sunrise: List<String?>? = null,
    val sunset: List<String?>? = null,
    @SerialName("uv_index_max") val uvIndexMax: List<Double?>? = null,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = null,
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?>? = null,
)
