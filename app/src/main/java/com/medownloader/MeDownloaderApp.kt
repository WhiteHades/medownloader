package com.medownloader

import android.app.Application
import com.medownloader.di.ServiceLocator

class MeDownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()

        ServiceLocator.initialize(this)
        ServiceLocator.providePremiumRepository().initialize()
    }
}
