package com.skyora.weather.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.skyora.weather.domain.model.DeviceLocationResult
import com.skyora.weather.domain.model.Location
import com.skyora.weather.domain.model.LocationFailureReason
import com.skyora.weather.domain.repository.DeviceLocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume
import android.location.Location as AndroidLocation

/**
 * Device location using the platform LocationManager only — no Play Services
 * dependency, so SKYORA works on de-Googled and low-end devices too.
 *
 * A recent last-known fix is preferred for speed; otherwise a single fresh fix
 * is requested with a timeout. Reverse geocoding is best-effort: if it fails we
 * still return the coordinates so weather keeps working.
 */
class AndroidDeviceLocationRepository(
    private val context: Context,
) : DeviceLocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(hasFinePermission: Boolean): DeviceLocationResult =
        withContext(Dispatchers.IO) {
            val permission = if (hasFinePermission) {
                Manifest.permission.ACCESS_FINE_LOCATION
            } else {
                Manifest.permission.ACCESS_COARSE_LOCATION
            }
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return@withContext DeviceLocationResult.Failure(LocationFailureReason.PERMISSION_DENIED)
            }

            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext DeviceLocationResult.Failure(LocationFailureReason.UNAVAILABLE)

            val enabledProviders = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
            val usableProviders = enabledProviders.filter {
                it == LocationManager.GPS_PROVIDER || it == LocationManager.NETWORK_PROVIDER ||
                    it == LocationManager.PASSIVE_PROVIDER
            }
            if (usableProviders.isEmpty()) {
                return@withContext DeviceLocationResult.Failure(LocationFailureReason.SERVICES_DISABLED)
            }

            val lastKnown = bestLastKnown(manager, usableProviders)
            val recent = lastKnown?.takeIf { System.currentTimeMillis() - it.time < RECENT_FIX_MS }
            val androidLocation = recent
                ?: requestSingleFix(manager, usableProviders, hasFinePermission)
                ?: lastKnown

            if (androidLocation == null) {
                return@withContext DeviceLocationResult.Failure(LocationFailureReason.TIMEOUT)
            }

            DeviceLocationResult.Success(
                location = androidLocation.toDomainLocation(),
                isApproximate = !hasFinePermission,
            )
        }

    @SuppressLint("MissingPermission")
    private fun bestLastKnown(
        manager: LocationManager,
        providers: List<String>,
    ): AndroidLocation? = providers
        .mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }

    private suspend fun requestSingleFix(
        manager: LocationManager,
        availableProviders: List<String>,
        hasFinePermission: Boolean,
    ): AndroidLocation? {
        val providers = buildList {
            if (hasFinePermission) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (!hasFinePermission) add(LocationManager.PASSIVE_PROVIDER)
        }.filter { it in availableProviders }

        for (provider in providers) {
            val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestCurrentLocation(manager, provider)
            } else {
                requestLegacySingleUpdate(manager, provider)
            }
            if (location != null) return location
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(
        manager: LocationManager,
        provider: String,
    ): AndroidLocation? = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            try {
                manager.getCurrentLocation(
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } catch (securityException: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (illegalArgumentException: IllegalArgumentException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private suspend fun requestLegacySingleUpdate(
        manager: LocationManager,
        provider: String,
    ): AndroidLocation? = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: AndroidLocation) {
                    if (continuation.isActive) continuation.resume(location)
                }

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            try {
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            } catch (securityException: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            } catch (illegalArgumentException: IllegalArgumentException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    private fun AndroidLocation.toDomainLocation(): Location {
        val place = reverseGeocode(latitude, longitude)
        return place?.copy(
            latitude = latitude,
            longitude = longitude,
        ) ?: Location(
            id = Location.deviceLocationId(latitude, longitude),
            name = "",
            admin1 = null,
            country = "",
            latitude = latitude,
            longitude = longitude,
            timezone = ZoneId.systemDefault().id,
            isDeviceLocation = true,
        )
    }

    private fun reverseGeocode(latitude: Double, longitude: Double): Location? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address: Address = runCatching {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)
        }.getOrNull()?.firstOrNull() ?: return null

        val name = address.locality?.takeIf { it.isNotBlank() }
            ?: address.subAdminArea?.takeIf { it.isNotBlank() }
            ?: address.adminArea?.takeIf { it.isNotBlank() }
            ?: ""
        return Location(
            id = Location.deviceLocationId(latitude, longitude),
            name = name,
            admin1 = address.adminArea?.takeIf { it.isNotBlank() && !it.equals(name, ignoreCase = true) },
            country = address.country.orEmpty(),
            countryCode = address.countryCode,
            latitude = latitude,
            longitude = longitude,
            timezone = ZoneId.systemDefault().id,
            isDeviceLocation = true,
        )
    }

    private companion object {
        const val PROVIDER_TIMEOUT_MS = 6_000L
        const val RECENT_FIX_MS = 5 * 60 * 1000L
    }
}
