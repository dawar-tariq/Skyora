package com.skyora.weather.data.repository

import com.skyora.weather.data.remote.OpenMeteoGeocodingApi
import com.skyora.weather.data.remote.dto.GeocodingResultDto
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.repository.CitySearchResult
import com.skyora.weather.domain.repository.GeocodingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.util.Locale

class GeocodingRepositoryImpl(
    private val api: OpenMeteoGeocodingApi,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeocodingRepository {

    private data class CacheEntry(val results: List<Location>, val storedAtMillis: Long)

    private val cache = object : LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }
    private val cacheGuard = Any()

    override suspend fun searchCities(query: String): CitySearchResult {
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.length < MIN_QUERY_LENGTH) {
            return CitySearchResult.Success(emptyList())
        }

        val now = clock.millis()
        synchronized(cacheGuard) {
            val cached = cache[normalized]
            if (cached != null && now - cached.storedAtMillis < CACHE_TTL_MS) {
                return CitySearchResult.Success(cached.results)
            }
        }

        return withContext(ioDispatcher) {
            try {
                val response = api.search(name = query.trim(), count = 10)
                val locations = response.results
                    .orEmpty()
                    .mapNotNull { it.toLocation() }
                    .fold(mutableListOf<Location>()) { acc, location ->
                        if (acc.none { it.isSamePlaceAs(location) }) acc.add(location)
                        acc
                    }
                    .take(10)
                synchronized(cacheGuard) {
                    cache[normalized] = CacheEntry(locations, now)
                }
                CitySearchResult.Success(locations)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                CitySearchResult.Failure(throwable.toWeatherError())
            }
        }
    }

    private fun GeocodingResultDto.toLocation(): Location? {
        val name = name?.takeIf { it.isNotBlank() } ?: return null
        val lat = latitude?.takeIf { it.isFinite() && it in -90.0..90.0 } ?: return null
        val lon = longitude?.takeIf { it.isFinite() && it in -180.0..180.0 } ?: return null
        val stableId = id?.let { "geo:$it" }
            ?: "geo:" + String.format(Locale.US, "%.4f,%.4f", lat, lon)
        return Location(
            id = stableId,
            name = name,
            admin1 = admin1?.takeIf { it.isNotBlank() },
            country = country ?: countryCode ?: "",
            countryCode = countryCode,
            latitude = lat,
            longitude = lon,
            timezone = timezone,
            isDeviceLocation = false,
        )
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val MAX_CACHE_ENTRIES = 24
    }
}
