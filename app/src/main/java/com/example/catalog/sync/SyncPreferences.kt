package com.example.catalog.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.catalog.core.LastSync
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Pamięta ostatnią udaną synchronizację (tekst „Ostatnia synchronizacja: …” po restarcie apki). */
class SyncPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    fun lastSync(): LastSync? {
        val at = prefs.getLong(KEY_AT, -1L)
        if (at < 0) return null
        return LastSync(count = prefs.getInt(KEY_COUNT, 0), at = at)
    }

    /** Bieżąca wartość + zmiany (np. po synchronizacji okresowej, której status nie jest śledzony w UI). */
    fun lastSyncFlow(): Flow<LastSync?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(lastSync()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(lastSync())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun saveLastSync(count: Int, at: Long) {
        prefs.edit {
            putInt(KEY_COUNT, count)
            putLong(KEY_AT, at)
        }
    }

    private companion object {
        const val KEY_COUNT = "last_sync_count"
        const val KEY_AT = "last_sync_at"
    }
}
