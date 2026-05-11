package com.medownloader

import android.app.Application
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.medownloader.di.ServiceLocator

class MeDownloaderApp : Application() {
    
    companion object {
        private const val TAG = "MeDownloaderApp"
    }

    override fun onCreate() {
        super.onCreate()
        
        ServiceLocator.initialize(this)
        ServiceLocator.providePremiumRepository().initialize()
        
        initPython()
    }

    private fun initPython() {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
            }
            val py = Python.getInstance()
            val ytdlp = py.getModule("yt_dlp.version")
            val version = ytdlp.callAttr("__version__").toString()
            Log.i(TAG, "yt-dlp $version ready")
        } catch (e: Exception) {
            Log.w(TAG, "python runtime unavailable: ${e.message}")
        }
    }
}
