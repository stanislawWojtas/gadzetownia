package com.example.catalog.data

import android.content.Context
import androidx.core.content.edit

/** Skąd pochodzą dane: prawdziwe API albo pliki z assets (bez internetu). */
enum class ApiMode { REMOTE, OFFLINE_ASSETS }

/** Symulowany błąd sieci (tylko debug). */
enum class ChaosMode { NONE, HTTP_503, TIMEOUT, NO_NETWORK }

/**
 * Ustawienia z menu debug, trzymane w SharedPreferences.
 * Interceptory czytają je przy każdym żądaniu, więc zmiana działa bez restartu.
 */
class DebugSettings(context: Context) {

    private val prefs = context.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)

    var apiMode: ApiMode
        get() = prefs.getString(KEY_API_MODE, null)
            ?.let { runCatching { ApiMode.valueOf(it) }.getOrNull() }
            ?: ApiMode.REMOTE
        set(value) = prefs.edit { putString(KEY_API_MODE, value.name) }

    var chaosMode: ChaosMode
        get() = prefs.getString(KEY_CHAOS_MODE, null)
            ?.let { runCatching { ChaosMode.valueOf(it) }.getOrNull() }
            ?: ChaosMode.NONE
        set(value) = prefs.edit { putString(KEY_CHAOS_MODE, value.name) }

    private companion object {
        const val KEY_API_MODE = "api_mode"
        const val KEY_CHAOS_MODE = "chaos_mode"
    }
}
