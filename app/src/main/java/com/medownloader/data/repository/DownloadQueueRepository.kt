package com.medownloader.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.medownloader.data.engine.EngineType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.queueDataStore by preferencesDataStore(name = "download_queue")

/**
 * Persists the pending download queue so it survives app/process death.
 * On next launch, the repository can restore queued items and resume them.
 */
class DownloadQueueRepository(private val context: Context) : DownloadQueueStore {

    private object Keys {
        val QUEUE = stringPreferencesKey("pending_queue")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val queue: Flow<List<QueuedDownload>> = context.queueDataStore.data.map { prefs ->
        val raw = prefs[Keys.QUEUE] ?: return@map emptyList()
        runCatching {
            json.decodeFromString(ListSerializer(QueuedDownload.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    override suspend fun save(items: List<QueuedDownload>) {
        context.queueDataStore.edit { prefs ->
            prefs[Keys.QUEUE] = json.encodeToString(
                ListSerializer(QueuedDownload.serializer()),
                items
            )
        }
    }

    override suspend fun clear() {
        context.queueDataStore.edit { prefs ->
            prefs.remove(Keys.QUEUE)
        }
    }

    override suspend fun snapshot(): List<QueuedDownload> = queue.first()
}

@Serializable
data class QueuedDownload(
    val url: String,
    val filename: String? = null,
    val engineType: String // EngineType.name
) {
    fun toEngineType(): EngineType = try {
        EngineType.valueOf(engineType)
    } catch (_: Exception) {
        EngineType.ARIA2C
    }
}
