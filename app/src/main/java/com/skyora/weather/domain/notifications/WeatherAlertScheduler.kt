package com.skyora.weather.domain.notifications

import com.skyora.weather.domain.model.Location

/**
 * Extension point for the future notification feature.
 *
 * Notifications are deliberately out of scope in this release. Keeping the
 * contract here means a WorkManager + FCM implementation can be added later
 * without coupling the weather repository or UI to a notification provider.
 */
interface WeatherAlertScheduler {
    suspend fun syncSchedule(homeCity: Location?)

    suspend fun cancelAll()
}

object NoOpWeatherAlertScheduler : WeatherAlertScheduler {
    override suspend fun syncSchedule(homeCity: Location?) = Unit

    override suspend fun cancelAll() = Unit
}
