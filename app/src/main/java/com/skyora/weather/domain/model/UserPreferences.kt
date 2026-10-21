package com.skyora.weather.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT,
    ;

    val symbol: String
        get() = if (this == CELSIUS) "°C" else "°F"

    val windUnit: String
        get() = if (this == CELSIUS) "km/h" else "mph"
}

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val onboardingCompleted: Boolean = false,
    val automaticLocationEnabled: Boolean = true,
    val selectedLocation: Location = DefaultLocations.Srinagar,
    val defaultCity: Location = DefaultLocations.Srinagar,
    val recentSearches: List<Location> = emptyList(),
)
