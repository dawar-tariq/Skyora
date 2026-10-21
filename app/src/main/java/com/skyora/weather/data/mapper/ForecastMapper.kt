package com.skyora.weather.data.mapper

import com.skyora.weather.core.util.resolveZoneId
import com.skyora.weather.data.remote.dto.DailyDto
import com.skyora.weather.data.remote.dto.ForecastResponseDto
import com.skyora.weather.data.remote.dto.HourlyDto
import com.skyora.weather.domain.model.CurrentWeather
import com.skyora.weather.domain.model.DailyForecast
import com.skyora.weather.domain.model.HourlyForecast
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.domain.model.localDateTimeToInstant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Maps the Open-Meteo payload onto immutable, unit-stable domain models.
 *
 * Missing or implausible values are dropped rather than defaulted to
 * misleading numbers. If essential data (current temperature or a complete
 * forecast day) is missing, null is returned and the UI shows an error state.
 */
class ForecastMapper {

    fun map(dto: ForecastResponseDto, location: Location, fetchedAtMillis: Long): WeatherSnapshot? {
        val currentDto = dto.current ?: return null
        val currentTemperature = currentDto.temperature.plausibleTemperature() ?: return null

        val utcOffsetSeconds = dto.utcOffsetSeconds ?: 0
        val timezone = dto.timezone?.takeIf { it.isNotBlank() } ?: "UTC"
        val zone = resolveZoneId(dto.timezone, utcOffsetSeconds)
        val now = Instant.ofEpochMilli(fetchedAtMillis)
        val currentInstant = localDateTimeToInstant(currentDto.time, zone) ?: now

        val hourly = mapHourly(dto.hourly, zone)
        val upcoming = hourly
            .filter { !Instant.ofEpochMilli(it.timeEpochMillis).isBefore(now.minusSeconds(3600)) }
            .take(24)

        val daily = mapDaily(dto.daily, zone)
        if (daily.isEmpty()) return null

        val currentVisibility = currentHourlyIndex(dto.hourly, zone, now)?.let { index ->
            dto.hourly?.visibility?.getOrNull(index).finiteOrNull()
        }

        return WeatherSnapshot(
            location = location,
            timezone = timezone,
            utcOffsetSeconds = utcOffsetSeconds,
            timezoneAbbreviation = dto.timezoneAbbreviation,
            elevationMeters = dto.elevation.finiteOrNull(),
            current = CurrentWeather(
                timeEpochMillis = currentInstant.toEpochMilli(),
                temperatureC = currentTemperature,
                feelsLikeC = currentDto.apparentTemperature.plausibleTemperature() ?: currentTemperature,
                weatherCode = currentDto.weatherCode ?: -1,
                isDay = when (currentDto.isDay) {
                    null -> currentInstant.atZone(zone).hour in 6..18
                    else -> currentDto.isDay != 0
                },
                humidityPercent = currentDto.relativeHumidity?.coerceIn(0, 100),
                windSpeedKmh = currentDto.windSpeed.nonNegativeOrNull(),
                windDirectionDegrees = currentDto.windDirection?.let { ((it % 360) + 360) % 360 },
                windGustKmh = currentDto.windGusts.nonNegativeOrNull(),
                pressureHpa = currentDto.pressureMsl?.takeIf { it.isFinite() && it > 0 },
                cloudCoverPercent = currentDto.cloudCover?.coerceIn(0, 100),
                precipitationMm = currentDto.precipitation.nonNegativeOrNull(),
                visibilityMeters = currentVisibility,
            ),
            hourly = upcoming,
            daily = daily,
            fetchedAtEpochMillis = fetchedAtMillis,
            schemaVersion = 1,
        )
    }

    private fun mapHourly(dto: HourlyDto?, zone: ZoneId): List<HourlyForecast> {
        val times = dto?.time ?: return emptyList()
        val temps = dto.temperature ?: return emptyList()
        return times.indices.mapNotNull { index ->
            val instant = localDateTimeToInstant(times.getOrNull(index), zone) ?: return@mapNotNull null
            val temperature = temps.getOrNull(index).plausibleTemperature() ?: return@mapNotNull null
            HourlyForecast(
                timeEpochMillis = instant.toEpochMilli(),
                temperatureC = temperature,
                weatherCode = dto.weatherCode?.getOrNull(index) ?: -1,
                isDay = (dto.isDay?.getOrNull(index) ?: 1) != 0,
                precipitationProbability = dto.precipitationProbability?.getOrNull(index)?.coerceIn(0, 100),
                feelsLikeC = dto.apparentTemperature?.getOrNull(index).plausibleTemperature(),
                humidityPercent = dto.relativeHumidity?.getOrNull(index)?.coerceIn(0, 100),
            )
        }
    }

    private fun mapDaily(dto: DailyDto?, zone: ZoneId): List<DailyForecast> {
        val times = dto?.time ?: return emptyList()
        val maxima = dto.temperatureMax ?: return emptyList()
        val minima = dto.temperatureMin ?: return emptyList()
        return times.indices.mapNotNull { index ->
            val date = times.getOrNull(index)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (runCatching { LocalDate.parse(date) }.isFailure) return@mapNotNull null
            val max = maxima.getOrNull(index).plausibleTemperature() ?: return@mapNotNull null
            val min = minima.getOrNull(index).plausibleTemperature() ?: return@mapNotNull null
            if (max < min) return@mapNotNull null
            DailyForecast(
                dateIso = date,
                weatherCode = dto.weatherCode?.getOrNull(index) ?: -1,
                tempMaxC = max,
                tempMinC = min,
                sunriseEpochMillis = localDateTimeToInstant(dto.sunrise?.getOrNull(index), zone)?.toEpochMilli(),
                sunsetEpochMillis = localDateTimeToInstant(dto.sunset?.getOrNull(index), zone)?.toEpochMilli(),
                precipitationProbabilityMax = dto.precipitationProbabilityMax?.getOrNull(index)?.coerceIn(0, 100),
                uvIndexMax = dto.uvIndexMax?.getOrNull(index)?.takeIf { it.isFinite() && it in 0.0..20.0 },
                windSpeedMaxKmh = dto.windSpeedMax?.getOrNull(index).nonNegativeOrNull(),
            )
        }
    }

    /**
     * The hourly slot that best represents "now": the most recent slot at or
     * before the fetch time, or the first upcoming slot when the payload starts
     * in the future. Using the *next* slot would show visibility/UV values from
     * an hour that has not happened yet.
     */
    private fun currentHourlyIndex(dto: HourlyDto?, zone: ZoneId, now: Instant): Int? {
        val times = dto?.time ?: return null
        if (times.isEmpty()) return null
        var currentIndex: Int? = null
        var firstUpcomingIndex: Int? = null
        times.forEachIndexed { index, raw ->
            val instant = localDateTimeToInstant(raw, zone) ?: return@forEachIndexed
            if (!instant.isAfter(now)) {
                currentIndex = index
            } else if (firstUpcomingIndex == null) {
                firstUpcomingIndex = index
            }
        }
        return currentIndex ?: firstUpcomingIndex
    }

    private fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }

    private fun Double?.nonNegativeOrNull(): Double? = this?.takeIf { it.isFinite() && it >= 0 }

    private fun Double?.plausibleTemperature(): Double? =
        this?.takeIf { it.isFinite() && it in -90.0..60.0 }
}
