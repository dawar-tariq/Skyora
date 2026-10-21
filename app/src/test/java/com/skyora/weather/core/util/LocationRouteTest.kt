package com.skyora.weather.core.util

import com.skyora.weather.domain.model.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

/**
 * The city screen is keyed by a Base64 route payload, so this guards the
 * round-trip for non-ASCII city names and rejects corrupted or out-of-range
 * payloads instead of opening a nonsense location.
 */
class LocationRouteTest {

    private fun location(
        id: String = "geo:2657896",
        name: String = "Zürich",
        admin1: String? = "Kanton Zürich",
        country: String = "Switzerland",
        countryCode: String? = "CH",
        latitude: Double = 47.3667,
        longitude: Double = 8.55,
        timezone: String? = "Europe/Zurich",
        isDeviceLocation: Boolean = false,
    ) = Location(
        id = id,
        name = name,
        admin1 = admin1,
        country = country,
        countryCode = countryCode,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
        isDeviceLocation = isDeviceLocation,
    )

    @Test
    fun `round trip preserves every field`() {
        val original = location()
        val decoded = decodeLocationFromRoute(original.encodeForRoute())

        assertEquals(original, decoded)
    }

    @Test
    fun `route payload is URL safe`() {
        val encoded = location().encodeForRoute()

        assertEquals(encoded, encoded.replace("+", "-").replace("/", "_"))
        assertEquals(encoded, encoded.replace("=", ""))
    }

    @Test
    fun `device locations keep their flag`() {
        val original = location(
            id = "gps:34.084,74.797",
            name = "Srinagar",
            admin1 = null,
            country = "India",
            countryCode = null,
            latitude = 34.084,
            longitude = 74.797,
            timezone = null,
            isDeviceLocation = true,
        )

        val decoded = decodeLocationFromRoute(original.encodeForRoute())

        assertNotNull(decoded)
        assertEquals(true, decoded?.isDeviceLocation)
        assertNull(decoded?.admin1)
        assertNull(decoded?.countryCode)
    }

    @Test
    fun `garbage input decodes to null instead of throwing`() {
        assertNull(decodeLocationFromRoute("not base64 at all"))
        assertNull(decodeLocationFromRoute(""))
    }

    @Test
    fun `out of range coordinates are rejected`() {
        val payload = listOf(
            "geo:1",
            "Nowhere",
            "",
            "Atlantis",
            "",
            "123.4",
            "8.5",
            "UTC",
            "false",
        ).joinToString("\u001F")
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(Charsets.UTF_8))

        assertNull(decodeLocationFromRoute(encoded))
    }

    @Test
    fun `payload with missing fields is rejected`() {
        val payload = listOf("geo:1", "Nowhere").joinToString("\u001F")
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(Charsets.UTF_8))

        assertNull(decodeLocationFromRoute(encoded))
    }
}
