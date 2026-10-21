package com.skyora.weather.data.remote

import com.skyora.weather.data.remote.dto.ForecastResponseDto
import com.skyora.weather.data.remote.dto.GeocodingResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("forecast_hours") forecastHours: Int = 25,
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
        @Query("precipitation_unit") precipitationUnit: String = "mm",
    ): ForecastResponseDto

    companion object {
        const val CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation," +
                "weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m"

        const val HOURLY_FIELDS =
            "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code," +
                "precipitation_probability,is_day,visibility"

        const val DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset," +
                "uv_index_max,precipitation_probability_max,wind_speed_10m_max"
    }
}

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponseDto
}
