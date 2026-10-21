package com.skyora.weather.domain.model

/**
 * Seed locations. Coordinates are stored with each entry so a weather request
 * never depends on a fresh geocoding lookup.
 */
object DefaultLocations {
    val Srinagar = Location(
        id = "geo:1255634",
        name = "Srinagar",
        admin1 = "Jammu and Kashmir",
        country = "India",
        countryCode = "IN",
        latitude = 34.0837,
        longitude = 74.7973,
        timezone = "Asia/Kolkata",
    )

    val NewDelhi = Location(
        id = "geo:1261481",
        name = "New Delhi",
        admin1 = "Delhi",
        country = "India",
        countryCode = "IN",
        latitude = 28.6139,
        longitude = 77.2090,
        timezone = "Asia/Kolkata",
    )

    val Mumbai = Location(
        id = "geo:1275339",
        name = "Mumbai",
        admin1 = "Maharashtra",
        country = "India",
        countryCode = "IN",
        latitude = 19.0760,
        longitude = 72.8777,
        timezone = "Asia/Kolkata",
    )

    val Bengaluru = Location(
        id = "geo:1277333",
        name = "Bengaluru",
        admin1 = "Karnataka",
        country = "India",
        countryCode = "IN",
        latitude = 12.9716,
        longitude = 77.5946,
        timezone = "Asia/Kolkata",
    )

    val Dubai = Location(
        id = "geo:292223",
        name = "Dubai",
        admin1 = "Dubai",
        country = "United Arab Emirates",
        countryCode = "AE",
        latitude = 25.0772,
        longitude = 55.3093,
        timezone = "Asia/Dubai",
    )

    val London = Location(
        id = "geo:2643743",
        name = "London",
        admin1 = "England",
        country = "United Kingdom",
        countryCode = "GB",
        latitude = 51.5085,
        longitude = -0.1257,
        timezone = "Europe/London",
    )

    val Tokyo = Location(
        id = "geo:1850147",
        name = "Tokyo",
        admin1 = "Tokyo",
        country = "Japan",
        countryCode = "JP",
        latitude = 35.6895,
        longitude = 139.6917,
        timezone = "Asia/Tokyo",
    )

    val NewYork = Location(
        id = "geo:5128581",
        name = "New York",
        admin1 = "New York",
        country = "United States",
        countryCode = "US",
        latitude = 40.7143,
        longitude = -74.0060,
        timezone = "America/New_York",
    )

    val Sydney = Location(
        id = "geo:2147714",
        name = "Sydney",
        admin1 = "New South Wales",
        country = "Australia",
        countryCode = "AU",
        latitude = -33.8679,
        longitude = 151.2073,
        timezone = "Australia/Sydney",
    )

    val popular: List<Location> = listOf(Mumbai, Bengaluru, London, Tokyo, Dubai, NewYork)

    val all: List<Location> = listOf(
        Srinagar, NewDelhi, Mumbai, Bengaluru, Dubai, London, Tokyo, NewYork, Sydney,
    )

    val fallback: Location = Srinagar
}
