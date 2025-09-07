package com.example.DhoonHub.storage

import android.content.Context
import androidx.core.content.edit

class SettingsStorage private constructor(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun setLanguage(language: String) {
        prefs.edit { putString(KEY_LANGUAGE, language) }
    }

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANG) ?: DEFAULT_LANG

    fun setOfflineMode(isOffline: Boolean) {
        prefs.edit { putBoolean(KEY_OFFLINE_MODE, isOffline) }
    }

    fun getOfflineMode(): Boolean = prefs.getBoolean(KEY_OFFLINE_MODE, false)

    companion object {
        private const val PREF_FILE = "settings_prefs"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_OFFLINE_MODE = "offline_mode"
        private const val DEFAULT_LANG = "English"
        @Volatile private var instance: SettingsStorage? = null
        fun getInstance(context: Context): SettingsStorage =
            instance ?: synchronized(this) {
                instance ?: SettingsStorage(context.applicationContext).also { instance = it }
            }
    }
}