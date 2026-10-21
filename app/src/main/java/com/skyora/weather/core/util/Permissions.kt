package com.skyora.weather.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Central permission names so screens never spell them by hand. */
const val LOCATION_FINE = Manifest.permission.ACCESS_FINE_LOCATION
const val LOCATION_COARSE = Manifest.permission.ACCESS_COARSE_LOCATION

/** Android 13+ runtime permission for local weather notifications. */
const val NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS

val LOCATION_PERMISSIONS: Array<String> = arrayOf(LOCATION_FINE, LOCATION_COARSE)

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.hasFineLocationPermission(): Boolean = hasPermission(LOCATION_FINE)

fun Context.hasCoarseLocationPermission(): Boolean = hasPermission(LOCATION_COARSE)

fun Context.hasAnyLocationPermission(): Boolean =
    hasFineLocationPermission() || hasCoarseLocationPermission()

fun Context.isLocationPermissionPermanentlyDenied(): Boolean =
    !hasAnyLocationPermission() && !hasPermission(LOCATION_FINE)

/**
 * Android only shows a rationale when the OS is still willing to display the
 * permission dialog. Once a denial is permanent this turns false, which is the
 * signal to send the user to system settings instead of asking again.
 */
fun Activity.shouldShowLocationRationale(): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, LOCATION_FINE) ||
        ActivityCompat.shouldShowRequestPermissionRationale(this, LOCATION_COARSE)
