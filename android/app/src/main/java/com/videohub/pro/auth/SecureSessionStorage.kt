package com.videohub.pro.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure Session Storage — stores platform session data using Android EncryptedSharedPreferences.
 *
 * Security:
 * - Uses Android Keystore for key generation
 * - EncryptedSharedPreferences with AES-256-GCM
 * - Never logs cookies/tokens/passwords
 * - Each platform's data is stored under a separate key namespace
 */
@Singleton
class SecureSessionStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "SecureSessionStorage"
        private const val PREFS_FILE_NAME = "videohub_secure_sessions"
        private const val KEY_PREFIX = "session_"
        private const val IV_PREFIX = "iv_"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /**
     * Store session data for a platform securely.
     * @param platformId Platform identifier (e.g., "youtube", "facebook")
     * @param sessionData JSON string containing cookies, tokens, etc.
     */
    fun storeSession(platformId: String, sessionData: String) {
        try {
            encryptedPrefs.edit()
                .putString("$KEY_PREFIX$platformId", sessionData)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store session for $platformId", e)
        }
    }

    /**
     * Retrieve session data for a platform.
     * @return JSON string or null if no session exists
     */
    fun getSession(platformId: String): String? {
        return try {
            encryptedPrefs.getString("$KEY_PREFIX$platformId", null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve session for $platformId", e)
            null
        }
    }

    /**
     * Check if a session exists for a platform.
     */
    fun hasSession(platformId: String): Boolean {
        return try {
            encryptedPrefs.contains("$KEY_PREFIX$platformId")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete session for a specific platform.
     * Does NOT affect other platforms' sessions.
     */
    fun deleteSession(platformId: String) {
        try {
            encryptedPrefs.edit()
                .remove("$KEY_PREFIX$platformId")
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete session for $platformId", e)
        }
    }

    /**
     * Delete ALL platform sessions.
     */
    fun deleteAllSessions() {
        try {
            val keys = encryptedPrefs.all.keys.filter { it.startsWith(KEY_PREFIX) }
            val editor = encryptedPrefs.edit()
            keys.forEach { editor.remove(it) }
            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all sessions", e)
        }
    }

    /**
     * Get list of platforms with active sessions.
     */
    fun getActivePlatforms(): List<String> {
        return try {
            encryptedPrefs.all.keys
                .filter { it.startsWith(KEY_PREFIX) }
                .map { it.removePrefix(KEY_PREFIX) }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
