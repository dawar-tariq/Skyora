package com.skyora.weather.data.repository

import com.skyora.weather.data.local.WeatherCacheStore
import com.skyora.weather.data.mapper.ForecastMapper
import com.skyora.weather.data.remote.OpenMeteoForecastApi
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.WeatherError
import com.skyora.weather.domain.model.WeatherResult
import com.skyora.weather.domain.model.WeatherSnapshot
import com.skyora.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock

class WeatherRepositoryImpl(
    private val api: OpenMeteoForecastApi,
    private val mapper: ForecastMapper,
    private val cacheStore: WeatherCacheStore,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /**
     * Invoked after a fresh snapshot is stored. AppContainer uses this to
     * refresh home-screen widgets without coupling this class to Android UI.
     */
    private val onSnapshotStored: ((WeatherSnapshot) -> Unit)? = null,
) : WeatherRepository {

    private val cache = MutableStateFlow<Map<String, WeatherSnapshot>>(emptyMap())
    private val cacheLoadMutex = Mutex()
    @Volatile
    private var cacheLoaded = false

    private val locks = mutableMapOf<String, Mutex>()
    private val locksGuard = Any()

    override fun observeWeather(locationId: String): Flow<WeatherSnapshot?> =
        cache.map { it[locationId] }.distinctUntilChanged()

    override suspend fun cachedWeather(locationId: String): WeatherSnapshot? {
        ensureCacheLoaded()
        return cache.value[locationId]
    }

    override suspend fun loadWeather(location: Location, forceRefresh: Boolean): WeatherResult =
        withContext(ioDispatcher) {
            ensureCacheLoaded()
            val lock = synchronized(locksGuard) { locks.getOrPut(location.id) { Mutex() } }
            lock.lock()
            try {
                val cached = cache.value[location.id]
                val now = clock.millis()
                if (!forceRefresh && cached != null &&
                    now - cached.fetchedAtEpochMillis < CACHE_FRESH_MS
                ) {
                    return@withContext WeatherResult.Success(cached, servedFromCache = true)
                }

                val dto = api.getForecast(latitude = location.latitude, longitude = location.longitude)
                val snapshot = mapper.map(dto, location, now)
                    ?: return@withContext WeatherResult.Failure(WeatherError.InvalidData, cached)

                cacheStore.write(snapshot)
                cache.update { it + (location.id to snapshot) }
                onSnapshotStored?.invoke(snapshot)
                WeatherResult.Success(snapshot)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                WeatherResult.Failure(throwable.toWeatherError(), cache.value[location.id])
            } finally {
                lock.unlock()
            }
        }

    override suspend fun clearCache() {
        withContext(ioDispatcher) {
            cacheStore.clear()
            cache.value = emptyMap()
        }
    }

    private suspend fun ensureCacheLoaded() {
        if (cacheLoaded) return
        cacheLoadMutex.lock()
        try {
            if (cacheLoaded) return
            val stored = withContext(ioDispatcher) { cacheStore.readAll() }
            cache.value = stored.associateBy { it.location.id }
            cacheLoaded = true
        } finally {
            cacheLoadMutex.unlock()
        }
    }

    private companion object {
        const val CACHE_FRESH_MS = 5 * 60 * 1000L
    }
}

fun Throwable.toWeatherError(): WeatherError = when (this) {
    is SocketTimeoutException -> WeatherError.Timeout
    is UnknownHostException, is ConnectException -> WeatherError.Offline
    is HttpException -> WeatherError.Server(code())
    is SerializationException -> WeatherError.InvalidData
    is IOException -> WeatherError.Offline
    else -> WeatherError.Unknown
}
