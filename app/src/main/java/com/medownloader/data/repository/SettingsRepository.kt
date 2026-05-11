package com.medownloader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.medownloader.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        val CONNECTION_LIMIT = intPreferencesKey("connection_limit")
        val SPLIT_COUNT = intPreferencesKey("split_count")
        val DNS_SERVERS = stringPreferencesKey("dns_servers")
        val ENABLE_DHT = booleanPreferencesKey("enable_dht")
        val DISK_CACHE_MB = intPreferencesKey("disk_cache_mb")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val DOWNLOAD_DIR_URI = stringPreferencesKey("download_dir_uri")
    }

    val appTheme: Flow<AppTheme> = context.dataStore.data
        .map { prefs ->
            try {
                AppTheme.valueOf(prefs[Keys.APP_THEME] ?: AppTheme.DEFAULT.name)
            } catch (e: Exception) {
                AppTheme.DEFAULT
            }
        }

    val maxConcurrentDownloads: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[Keys.MAX_CONCURRENT] ?: 3 }

    val connectionLimit: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[Keys.CONNECTION_LIMIT] ?: 4 }

    val splitCount: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[Keys.SPLIT_COUNT] ?: 8 }

    val dnsServers: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[Keys.DNS_SERVERS] ?: "8.8.8.8,8.8.4.4,1.1.1.1" }

    val enableDht: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[Keys.ENABLE_DHT] ?: true }

    val diskCacheMb: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[Keys.DISK_CACHE_MB] ?: 32 }

    val wifiOnly: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[Keys.WIFI_ONLY] ?: false }

    val downloadDirUri: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[Keys.DOWNLOAD_DIR_URI] }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_THEME] = theme.name
        }
    }

    suspend fun setMaxConcurrentDownloads(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_CONCURRENT] = count
        }
    }
    
    suspend fun setConnectionLimit(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CONNECTION_LIMIT] = count
        }
    }

    suspend fun setSplitCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SPLIT_COUNT] = count
        }
    }

    suspend fun setDnsServers(servers: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DNS_SERVERS] = servers
        }
    }

    suspend fun setEnableDht(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_DHT] = enabled
        }
    }

    suspend fun setDiskCacheMb(mb: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISK_CACHE_MB] = mb
        }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY] = enabled
        }
    }
    
    suspend fun setDownloadDirUri(uriString: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DOWNLOAD_DIR_URI] = uriString
        }
    }
}
