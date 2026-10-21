package com.skyora.weather

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skyora.weather.core.util.decodeLocationFromRoute
import com.skyora.weather.domain.model.Location
import com.skyora.weather.ui.navigation.SkyoraApp
import com.skyora.weather.ui.navigation.SkyoraRoutes
import com.skyora.weather.ui.theme.SkyoraTheme

class MainActivity : ComponentActivity() {

    /**
     * Deep-link target from a widget tap or a notification tap.
     *
     * Held in a Compose state so a tap that arrives while the activity is
     * already running (onNewIntent) still navigates instead of being ignored.
     */
    private var pendingLocation by mutableStateOf<Location?>(null)

    /**
     * Set when a widget's status line is tapped on a stale widget: that tap is a
     * question ("why is this old?"), so it opens the reliability page instead of
     * a forecast.
     */
    private var pendingWidgetReliability by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        pendingLocation = intent.readLocation()
        pendingWidgetReliability = intent.wantsWidgetReliability()
        val container = (application as SkyoraApplication).container
        setContent {
            SkyoraTheme {
                SkyoraApp(
                    container = container,
                    requestedLocation = pendingLocation,
                    onRequestedLocationHandled = { pendingLocation = null },
                    requestedRoute = if (pendingWidgetReliability) {
                        SkyoraRoutes.WIDGET_RELIABILITY
                    } else {
                        null
                    },
                    onRequestedRouteHandled = { pendingWidgetReliability = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.readLocation()?.let { pendingLocation = it }
        if (intent.wantsWidgetReliability()) pendingWidgetReliability = true
    }

    private fun Intent?.wantsWidgetReliability(): Boolean =
        this?.getBooleanExtra(EXTRA_OPEN_RELIABILITY, false) == true

    private fun Intent?.readLocation(): Location? = this
        ?.getStringExtra(EXTRA_LOCATION_PAYLOAD)
        ?.let(::decodeLocationFromRoute)

    companion object {
        /** Base64 (URL-safe) encoded Location; see core/util/LocationRoute.kt. */
        const val EXTRA_LOCATION_PAYLOAD = "com.skyora.weather.extra.LOCATION"

        /** Widget status-line tap: open Widget reliability. */
        const val EXTRA_OPEN_RELIABILITY = "com.skyora.weather.extra.OPEN_RELIABILITY"
    }
}
