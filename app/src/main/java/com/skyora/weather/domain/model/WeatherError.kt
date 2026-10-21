package com.skyora.weather.domain.model

/** Network / API failures mapped to friendly, stable UI states. */
sealed interface WeatherError {
    data object Offline : WeatherError
    data object Timeout : WeatherError
    data class Server(val statusCode: Int? = null) : WeatherError
    data object InvalidData : WeatherError
    data object Unknown : WeatherError
}

sealed interface WeatherResult {
    data class Success(
        val snapshot: WeatherSnapshot,
        /** True when no network call was made because the cache was still fresh. */
        val servedFromCache: Boolean = false,
    ) : WeatherResult

    data class Failure(
        val error: WeatherError,
        val cachedSnapshot: WeatherSnapshot? = null,
    ) : WeatherResult
}

sealed interface LocationAccessState {
    data object Idle : LocationAccessState
    data object Locating : LocationAccessState
    data class Success(val location: Location, val isApproximate: Boolean) : LocationAccessState
    data class PermissionRequired(val permanentDenial: Boolean = false) : LocationAccessState
    data class Error(val reason: LocationFailureReason) : LocationAccessState
}

enum class LocationFailureReason {
    PERMISSION_DENIED,
    SERVICES_DISABLED,
    TIMEOUT,
    UNAVAILABLE,
}

sealed interface DeviceLocationResult {
    data class Success(val location: Location, val isApproximate: Boolean) : DeviceLocationResult
    data class Failure(val reason: LocationFailureReason) : DeviceLocationResult
}
