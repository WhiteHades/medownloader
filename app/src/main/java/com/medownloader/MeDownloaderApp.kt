package com.medownloader

import android.app.Application
import com.medownloader.di.ServiceLocator

class MeDownloaderApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize DI container
        ServiceLocator.initialize(this)
        
        // Initialize RevenueCat
        ServiceLocator.providePremiumRepository().initialize()
    }
}
