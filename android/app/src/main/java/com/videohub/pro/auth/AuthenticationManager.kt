package com.videohub.pro.auth

import android.util.Log
import com.videohub.pro.resolver.AuthenticationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-platform session data — isolated for each platform.
 * Never mixed between platforms.
 */
data class PlatformSession(
    val platformId: String,
    val cookies: Map<String, String> = emptyMap(),
    val token: String? = null,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
)

/**
 * Authentication Manager — manages platform-specific authenticated sessions.
 *
 * Architecture:
 * - Each platform has its own isolated session
 * - Sessions are stored securely via SecureSessionStorage
 * - WebView login is used for user-initiated authentication
 * - ResolverManager requests the correct AuthenticationContext per platform
 * - Sessions can expire and be refreshed
 *
 * Security:
 * - Never logs cookies, tokens, or passwords
 * - Session data is encrypted at rest
 * - Logout from one platform does not affect others
 */
@Singleton
class AuthenticationManager @Inject constructor(
    private val secureSessionStorage: SecureSessionStorage,
) {
    companion object {
        private const val TAG = "AuthManager"
        private val SUPPORTED_PLATFORMS = listOf(
            "youtube", "facebook", "tiktok", "instagram", "x", "reddit",
        )

        // Login URLs for each platform
        val LOGIN_URLS = mapOf(
            "youtube" to "https://accounts.google.com/serviceLogin?continue=https://www.youtube.com",
            "facebook" to "https://www.facebook.com/login.php",
            "tiktok" to "https://www.tiktok.com/login",
            "instagram" to "https://www.instagram.com/accounts/login/",
            "x" to "https://twitter.com/i/flow/login",
            "reddit" to "https://www.reddit.com/login/",
        )

        // Cookie domains for each platform
        val COOKIE_DOMAINS = mapOf(
            "youtube" to listOf(".youtube.com", ".google.com", "accounts.google.com"),
            "facebook" to listOf(".facebook.com", "m.facebook.com"),
            "tiktok" to listOf(".tiktok.com"),
            "instagram" to listOf(".instagram.com"),
            "x" to listOf(".twitter.com", ".x.com"),
            "reddit" to listOf(".reddit.com"),
        )
    }

    /**
     * Get the authentication context for a platform.
     * Returns null if no session exists or session is expired.
     */
    fun getAuthContext(platformId: String): AuthenticationContext? {
        if (!SUPPORTED_PLATFORMS.contains(platformId)) return null

        val sessionJson = secureSessionStorage.getSession(platformId) ?: return null
        val session = parseSession(platformId, sessionJson) ?: return null

        // Check expiration
        if (isSessionExpired(session)) {
            Log.i(TAG, "Session expired for $platformId — clearing")
            secureSessionStorage.deleteSession(platformId)
            return null
        }

        return AuthenticationContext(
            platformId = session.platformId,
            sessionId = "${session.platformId}_${session.createdAt}",
            cookies = session.cookies,
            token = session.token,
            expiresAt = session.expiresAt,
        )
    }

    /**
     * Save a session after WebView login.
     * Called by AuthWebViewActivity when login is complete.
     */
    fun saveSession(platformId: String, cookies: Map<String, String>, token: String? = null) {
        if (!SUPPORTED_PLATFORMS.contains(platformId)) {
            Log.w(TAG, "Platform $platformId does not support authentication")
            return
        }

        val session = PlatformSession(
            platformId = platformId,
            cookies = cookies,
            token = token,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L), // 30 days default
        )

        val json = sessionToJson(session)
        secureSessionStorage.storeSession(platformId, json)
        Log.i(TAG, "Session saved for $platformId (cookies: ${cookies.size})")
    }

    /**
     * Check if a platform has an active (non-expired) session.
     */
    fun isLoggedIn(platformId: String): Boolean {
        return getAuthContext(platformId) != null
    }

    /**
     * Logout from a specific platform.
     * Does NOT affect other platforms.
     */
    fun logout(platformId: String) {
        secureSessionStorage.deleteSession(platformId)
        Log.i(TAG, "Logged out from $platformId")
    }

    /**
     * Clear all sessions for all platforms.
     */
    fun clearAllSessions() {
        secureSessionStorage.deleteAllSessions()
        Log.i(TAG, "All sessions cleared")
    }

    /**
     * Get list of platforms the user is logged into.
     */
    fun getLoggedInPlatforms(): List<String> {
        return SUPPORTED_PLATFORMS.filter { isLoggedIn(it) }
    }

    /**
     * Get all supported platforms with their login status.
     */
    fun getAllPlatformStatus(): List<PlatformAuthStatus> {
        return SUPPORTED_PLATFORMS.map { platformId ->
            PlatformAuthStatus(
                platformId = platformId,
                isLoggedIn = isLoggedIn(platformId),
                loginUrl = LOGIN_URLS[platformId],
            )
        }
    }

    /**
     * Get login URL for a platform.
     */
    fun getLoginUrl(platformId: String): String? {
        return LOGIN_URLS[platformId]
    }

    /**
     * Get cookie domains for a platform (used by WebView to filter cookies).
     */
    fun getCookieDomains(platformId: String): List<String> {
        return COOKIE_DOMAINS[platformId] ?: emptyList()
    }

    /**
     * Check if a platform supports authentication.
     */
    fun supportsAuth(platformId: String): Boolean {
        return SUPPORTED_PLATFORMS.contains(platformId)
    }

    // ============ Private helpers ============

    private fun isSessionExpired(session: PlatformSession): Boolean {
        val expiresAt = session.expiresAt ?: return false
        return System.currentTimeMillis() > expiresAt
    }

    private fun parseSession(platformId: String, json: String): PlatformSession? {
        return try {
            val obj = JSONObject(json)
            val cookies = mutableMapOf<String, String>()
            val cookiesObj = obj.optJSONObject("cookies")
            if (cookiesObj != null) {
                val keys = cookiesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    cookies[key] = cookiesObj.getString(key)
                }
            }
            PlatformSession(
                platformId = platformId,
                cookies = cookies,
                token = obj.optString("token", null),
                userId = obj.optString("userId", null),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                expiresAt = obj.optLong("expiresAt", 0).takeIf { it > 0 },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse session for $platformId", e)
            null
        }
    }

    private fun sessionToJson(session: PlatformSession): String {
        val obj = JSONObject()
        val cookiesObj = JSONObject()
        session.cookies.forEach { (k, v) -> cookiesObj.put(k, v) }
        obj.put("cookies", cookiesObj)
        session.token?.let { obj.put("token", it) }
        session.userId?.let { obj.put("userId", it) }
        obj.put("createdAt", session.createdAt)
        session.expiresAt?.let { obj.put("expiresAt", it) }
        return obj.toString()
    }
}

/**
 * Platform authentication status for UI display.
 */
data class PlatformAuthStatus(
    val platformId: String,
    val isLoggedIn: Boolean,
    val loginUrl: String?,
)
