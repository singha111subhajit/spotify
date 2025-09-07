package com.example.DhoonHub.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log

class TokenStorage private constructor(context: Context) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setToken(token: String?) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
        Log.d(TAG, "Token set: $token")
    }

    fun getToken(): String? {
        val token = sharedPreferences.getString(KEY_TOKEN, null)
        Log.d(TAG, "Token retrieved: $token")
        return token
    }

    fun setUsername(username: String?) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
        Log.d(TAG, "Username set: $username")
    }

    fun getUsername(): String? {
        val username = sharedPreferences.getString(KEY_USERNAME, null)
        Log.d(TAG, "Username retrieved: $username")
        return username
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "All preferences cleared.")
    }

    companion object {
        private const val PREF_FILE = "auth_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
        private const val TAG = "TokenStorage"

        @Volatile private var instance: TokenStorage? = null

        fun getInstance(context: Context): TokenStorage {
            return instance ?: synchronized(this) {
                instance ?: TokenStorage(context.applicationContext).also { instance = it }
            }
        }
    }
}