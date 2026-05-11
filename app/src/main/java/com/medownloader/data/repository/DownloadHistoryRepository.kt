package com.medownloader.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.medownloader.data.model.DownloadHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.historyDataStore by preferencesDataStore(name = "download_history")

class DownloadHistoryRepository(private val context: Context) {

    companion object {
        private const val MAX_ENTRIES = 500
    }

    private object Keys {
        val HISTORY = stringPreferencesKey("history_entries")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val history: Flow<List<DownloadHistoryEntry>> = context.historyDataStore.data.map { prefs ->
        val raw = prefs[Keys.HISTORY] ?: return@map emptyList()
        runCatching {
            json.decodeFromString(ListSerializer(DownloadHistoryEntry.serializer()), raw)
                .sortedByDescending { it.finishedAtEpochMs }
        }.getOrDefault(emptyList())
    }

    suspend fun append(entry: DownloadHistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = prefs[Keys.HISTORY]?.let { raw ->
                runCatching {
                    json.decodeFromString(ListSerializer(DownloadHistoryEntry.serializer()), raw)
                }.getOrDefault(emptyList())
            } ?: emptyList()

            val updated = (listOf(entry) + current)
                .distinctBy { it.id }
                .take(MAX_ENTRIES)

            prefs[Keys.HISTORY] = json.encodeToString(
                ListSerializer(DownloadHistoryEntry.serializer()),
                updated
            )
        }
    }

    suspend fun clear() {
        context.historyDataStore.edit { prefs ->
            prefs.remove(Keys.HISTORY)
        }
    }
}
