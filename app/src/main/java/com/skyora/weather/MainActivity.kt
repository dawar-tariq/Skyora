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
import com.skyora.weather.ui.theme.SkyoraTheme

class MainActivity : ComponentActivity() {

    /**
     * Deep-link target from a widget tap or a notification tap.
     *
     * Held in a Compose state so a tap that arrives while the activity is
     * already running (onNewIntent) still navigates instead of being ignored.
     */
    private var pendingLocation by mutableStateOf<Location?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        pendingLocation = intent.readLocation()
        val container = (application as SkyoraApplication).container
        setContent {
            SkyoraTheme {
                SkyoraApp(
                    container = container,
                    requestedLocation = pendingLocation,
                    onRequestedLocationHandled = { pendingLocation = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.readLocation()?.let { pendingLocation = it }
    }

    private fun Intent?.readLocation(): Location? = this
        ?.getStringExtra(EXTRA_LOCATION_PAYLOAD)
        ?.let(::decodeLocationFromRoute)

    companion object {
        /** Base64 (URL-safe) encoded Location; see core/util/LocationRoute.kt. */
        const val EXTRA_LOCATION_PAYLOAD = "com.skyora.weather.extra.LOCATION"
    }
}
