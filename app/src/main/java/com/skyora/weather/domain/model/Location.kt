package com.skyora.weather.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A place the app can show weather for.
 *
 * [id] is the stable identity used to de-duplicate saved cities:
 *  - `geo:<geocoding id>` for cities from Open-Meteo's geocoder
 *  - `gps:<lat>,<lon>` for the device location
 *
 * Coordinates are always stored, never just the display name, so forecasts stay
 * attached to the right place even when names repeat (for example "Delhi" in
 * India, the US, Canada and Australia).
 */
@Serializable
data class Location(
    val id: String,
    val name: String,
    val admin1: String? = null,
    val country: String = "",
    val countryCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val isDeviceLocation: Boolean = false,
) {
    /** "Jammu and Kashmir, India" without repeating the city name. */
    val subtitle: String
        get() = listOfNotNull(
            admin1?.takeIf { it.isNotBlank() && !it.equals(name, ignoreCase = true) },
            country.takeIf { it.isNotBlank() },
        ).distinctBy { it.lowercase(Locale.ROOT) }.joinToString(", ")

    fun distanceToKm(other: Location): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * Two locations are treated as the same place when their stable ids match,
     * they sit within a few kilometres of each other, or they are the same
     * named city in the same country within a wider radius. This keeps saved
     * cities and recents free of duplicates without ever silently replacing a
     * user's saved coordinate with a geocoder guess.
     */
    fun isSamePlaceAs(other: Location, toleranceKm: Double = 3.0): Boolean {
        if (id == other.id) return true
        val distance = distanceToKm(other)
        if (distance <= toleranceKm) return true
        return name.equals(other.name.trim(), ignoreCase = true) &&
            country.equals(other.country.trim(), ignoreCase = true) &&
            distance <= 25.0
    }

    companion object {
        fun deviceLocationId(latitude: Double, longitude: Double): String =
            "gps:" + String.format(Locale.US, "%.3f,%.3f", latitude, longitude)
    }
}
