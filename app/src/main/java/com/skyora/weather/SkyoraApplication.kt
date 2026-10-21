package com.skyora.weather

import android.app.Application
import com.skyora.weather.di.AppContainer

class SkyoraApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Channels, periodic widget refresh and preference observers.
        container.startBackgroundSync()
    }
}
