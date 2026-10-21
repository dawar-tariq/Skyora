package com.skyora.weather.core.util

import com.skyora.weather.domain.model.Location
import java.util.Base64

/**
 * City weather is a real navigation destination, not a global mutable "last
 * picked city". The Location is encoded into the route so it survives process
 * death and back-stack restoration. Base64 URL-safe output contains only
 * characters that are safe inside a Navigation route.
 */
private const val FIELD_SEPARATOR = '\u001F'

fun Location.encodeForRoute(): String {
    val payload = listOf(
        id,
        name,
        admin1.orEmpty(),
        country,
        countryCode.orEmpty(),
        latitude.toString(),
        longitude.toString(),
        timezone.orEmpty(),
        isDeviceLocation.toString(),
    ).joinToString(FIELD_SEPARATOR.toString())
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(payload.toByteArray(Charsets.UTF_8))
}

fun decodeLocationFromRoute(encoded: String): Location? = runCatching {
    val payload = String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
    val parts = payload.split(FIELD_SEPARATOR)
    if (parts.size < 9) return null
    val latitude = parts[5].toDouble()
    val longitude = parts[6].toDouble()
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    Location(
        id = parts[0],
        name = parts[1],
        admin1 = parts[2].ifBlank { null },
        country = parts[3],
        countryCode = parts[4].ifBlank { null },
        latitude = latitude,
        longitude = longitude,
        timezone = parts[7].ifBlank { null },
        isDeviceLocation = parts[8].toBooleanStrictOrNull() ?: false,
    )
}.getOrNull()
