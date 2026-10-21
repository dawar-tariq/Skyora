package com.skyora.weather.di

import android.content.Context
import com.skyora.weather.BuildConfig
import com.skyora.weather.core.network.NetworkMonitor
import com.skyora.weather.data.location.AndroidDeviceLocationRepository
import com.skyora.weather.data.local.PreferencesRepositoryImpl
import com.skyora.weather.data.local.SavedCitiesRepositoryImpl
import com.skyora.weather.data.local.SkyoraDataStores
import com.skyora.weather.data.local.WeatherCacheStore
import com.skyora.weather.data.mapper.ForecastMapper
import com.skyora.weather.data.remote.OpenMeteoForecastApi
import com.skyora.weather.data.remote.OpenMeteoGeocodingApi
import com.skyora.weather.data.repository.GeocodingRepositoryImpl
import com.skyora.weather.data.repository.WeatherRepositoryImpl
import com.skyora.weather.domain.location.LocationCoordinator
import com.skyora.weather.domain.notifications.NoOpWeatherAlertScheduler
import com.skyora.weather.domain.notifications.WeatherAlertScheduler
import com.skyora.weather.domain.repository.DeviceLocationRepository
import com.skyora.weather.domain.repository.GeocodingRepository
import com.skyora.weather.domain.repository.PreferencesRepository
import com.skyora.weather.domain.repository.SavedCitiesRepository
import com.skyora.weather.domain.repository.WeatherRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.time.Clock
import java.util.concurrent.TimeUnit

/**
 * Hand-rolled dependency container. Keeping construction explicit makes the
 * dependency graph readable and avoids an annotation processor for a small app.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.systemUTC()

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    private val preferenceDataStore by lazy { SkyoraDataStores.preferences(appContext) }
    private val cacheDataStore by lazy { SkyoraDataStores.weatherCache(appContext) }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }
            .build()
    }

    private val forecastApi: OpenMeteoForecastApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoForecastApi::class.java)
    }

    private val geocodingApi: OpenMeteoGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepositoryImpl(preferenceDataStore, json)
    }

    val savedCitiesRepository: SavedCitiesRepository by lazy {
        SavedCitiesRepositoryImpl(preferenceDataStore, json)
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepositoryImpl(
            api = forecastApi,
            mapper = ForecastMapper(),
            cacheStore = WeatherCacheStore(cacheDataStore, json),
            clock = clock,
        )
    }

    val geocodingRepository: GeocodingRepository by lazy {
        GeocodingRepositoryImpl(geocodingApi, clock)
    }

    val deviceLocationRepository: DeviceLocationRepository by lazy {
        AndroidDeviceLocationRepository(appContext)
    }

    val locationCoordinator: LocationCoordinator by lazy {
        LocationCoordinator(deviceLocationRepository, preferencesRepository)
    }

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(appContext) }

    /** Reserved for the future notification phase; a no-op for now. */
    val weatherAlertScheduler: WeatherAlertScheduler = NoOpWeatherAlertScheduler

    val appVersionName: String = BuildConfig.VERSION_NAME
}
