package com.skyora.weather.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponseDto(
    val results: List<GeocodingResultDto>? = null,
    @SerialName("generationtime_ms") val generationTimeMs: Double? = null,
)

@Serializable
data class GeocodingResultDto(
    val id: Long? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,
    @SerialName("feature_code") val featureCode: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val timezone: String? = null,
    val population: Long? = null,
    val country: String? = null,
    val countryId: Long? = null,
    val admin1: String? = null,
    val admin2: String? = null,
    val admin3: String? = null,
)
