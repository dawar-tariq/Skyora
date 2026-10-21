package com.skyora.weather.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.LocalDateTime

@Serializable
data class WeatherSnapshot(
    val location: Location,
    val timezone: String,
    val utcOffsetSeconds: Int,
    val timezoneAbbreviation: String? = null,
    val elevationMeters: Double? = null,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast> = emptyList(),
    val daily: List<DailyForecast> = emptyList(),
    val fetchedAtEpochMillis: Long,
    val provider: String = "Open-Meteo",
    val schemaVersion: Int = 1,
) {
    /**
     * Open-Meteo may return a fixed UTC offset name (for example "GMT+5:30")
     * instead of an IANA zone. Fall back to the numeric offset in that case.
     */
    fun zoneId(): ZoneId =
        runCatching { ZoneId.of(timezone) }
            .getOrElse { ZoneOffset.ofTotalSeconds(utcOffsetSeconds) }

    fun currentCondition(): WeatherCondition = WmoWeatherCodes.conditionFor(current.weatherCode)

    fun today(): DailyForecast? {
        val today = LocalDate.now(zoneId())
        return daily.firstOrNull { it.localDate() == today } ?: daily.firstOrNull()
    }

    fun fetchedAt(): Instant = Instant.ofEpochMilli(fetchedAtEpochMillis)
}

@Serializable
data class CurrentWeather(
    val timeEpochMillis: Long,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val humidityPercent: Int? = null,
    val windSpeedKmh: Double? = null,
    val windDirectionDegrees: Int? = null,
    val windGustKmh: Double? = null,
    val pressureHpa: Double? = null,
    val cloudCoverPercent: Int? = null,
    val precipitationMm: Double? = null,
    val visibilityMeters: Double? = null,
)

@Serializable
data class HourlyForecast(
    val timeEpochMillis: Long,
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val precipitationProbability: Int? = null,
    val feelsLikeC: Double? = null,
    val humidityPercent: Int? = null,
) {
    fun time(): Instant = Instant.ofEpochMilli(timeEpochMillis)
}

@Serializable
data class DailyForecast(
    val dateIso: String,
    val weatherCode: Int,
    val tempMaxC: Double,
    val tempMinC: Double,
    val sunriseEpochMillis: Long? = null,
    val sunsetEpochMillis: Long? = null,
    val precipitationProbabilityMax: Int? = null,
    val uvIndexMax: Double? = null,
    val windSpeedMaxKmh: Double? = null,
) {
    fun localDate(): LocalDate? = runCatching { LocalDate.parse(dateIso) }.getOrNull()

    fun sunrise(): Instant? = sunriseEpochMillis?.let(Instant::ofEpochMilli)

    fun sunset(): Instant? = sunsetEpochMillis?.let(Instant::ofEpochMilli)

    fun condition(): WeatherCondition = WmoWeatherCodes.conditionFor(weatherCode)
}

/** Formats a local wall-clock time from the API, interpreted in [zone]. */
fun localDateTimeToInstant(value: String?, zone: ZoneId): Instant? {
    if (value.isNullOrBlank()) return null
    val local = runCatching { LocalDateTime.parse(value).atZone(zone).toInstant() }.getOrNull()
    if (local != null) return local
    return runCatching {
        java.time.OffsetDateTime.parse(value).toInstant()
    }.getOrNull()
}
