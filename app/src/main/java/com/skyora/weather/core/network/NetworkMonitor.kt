package com.skyora.weather.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide connectivity signal used for offline banners and cached-data copy. */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    private val _isOnline = MutableStateFlow(readCurrentState())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = readCurrentState()
        }

        override fun onLost(network: Network) {
            _isOnline.value = readCurrentState()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            _isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    init {
        runCatching { connectivityManager?.registerDefaultNetworkCallback(callback) }
    }

    private fun readCurrentState(): Boolean {
        val manager = connectivityManager ?: return true
        val activeNetwork = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
