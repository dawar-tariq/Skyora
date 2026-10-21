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

/**
 * User choices for local weather notifications.
 *
 * All of these are local, scheduled notifications produced from the same
 * forecast the app displays. They are not remote push messages, and the
 * "severe weather" channel is intentionally absent until an official alert
 * source is integrated (see [SevereWeatherAvailability]).
 */
data class NotificationSettings(
    val dailyForecastEnabled: Boolean = false,
    /** Minutes after local midnight; 7:00 AM by default. */
    val dailyForecastMinutes: Int = 7 * 60,
    val rainAlertsEnabled: Boolean = false,
    /**
     * Precipitation-probability threshold (0-100). A rain alert is sent only
     * when an upcoming hour reaches this probability.
     */
    val rainThresholdPercent: Int = 60,
    /** How far ahead to look for rain, in hours. */
    val rainLookaheadHours: Int = 6,
    /** City used for notification forecasts; null means "the selected city". */
    val notificationLocation: Location? = null,
) {
    val anyEnabled: Boolean get() = dailyForecastEnabled || rainAlertsEnabled

    /** 7:00 AM -> "07:00". */
    val dailyForecastTimeLabel: String
        get() = "%02d:%02d".format(dailyForecastMinutes / 60, dailyForecastMinutes % 60)
}

/**
 * Severe-weather alerts need an official, attributable alert feed. SKYORA only
 * integrates Open-Meteo forecast data, which is not an official warning
 * service, so this stays explicitly unavailable rather than fabricating alerts
 * from ordinary forecasts.
 */
object SevereWeatherAvailability {
    const val SUPPORTED = false
    const val SOURCE_NAME = "Open-Meteo (forecast only)"
}

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val onboardingCompleted: Boolean = false,
    val automaticLocationEnabled: Boolean = true,
    val selectedLocation: Location = DefaultLocations.Srinagar,
    val defaultCity: Location = DefaultLocations.Srinagar,
    val recentSearches: List<Location> = emptyList(),
    val notifications: NotificationSettings = NotificationSettings(),
)
