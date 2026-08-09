package com.videohub.pro.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App Settings — يحفظ جميع الإعدادات في SharedPreferences.
 * كل تغيير يُحفظ فوراً ويُقرأ عند بدء التطبيق.
 */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("videohub_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_QUALITY = "default_quality"
        const val KEY_CONCURRENT = "concurrent_downloads"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_AUTO_RETRY = "auto_retry"
        const val KEY_THEME = "theme"
        const val KEY_ACCENT = "accent_index"
        const val KEY_LANGUAGE = "language"
        const val KEY_DOWNLOAD_PATH = "download_path"
        const val KEY_STORAGE_LIMIT = "storage_limit_gb"
        const val KEY_BANDWIDTH_LIMIT = "bandwidth_limit_mbps"
        const val KEY_SMART_SCHEDULING = "smart_scheduling"

        const val DEFAULT_QUALITY = "1080p"
        const val DEFAULT_CONCURRENT = 3
        const val DEFAULT_NOTIFICATIONS = true
        const val DEFAULT_AUTO_RETRY = true
        const val DEFAULT_THEME = "dark"
        const val DEFAULT_ACCENT = 0
        const val DEFAULT_LANGUAGE = "ar"
        const val DEFAULT_STORAGE_LIMIT = 32
        const val DEFAULT_BANDWIDTH_LIMIT = 0
        const val DEFAULT_SMART_SCHEDULING = true
    }

    fun getDefaultQuality(): String = prefs.getString(KEY_QUALITY, DEFAULT_QUALITY) ?: DEFAULT_QUALITY
    fun setDefaultQuality(value: String) = prefs.edit().putString(KEY_QUALITY, value).apply()

    fun getConcurrentDownloads(): Int = prefs.getInt(KEY_CONCURRENT, DEFAULT_CONCURRENT)
    fun setConcurrentDownloads(value: Int) = prefs.edit().putInt(KEY_CONCURRENT, value).apply()

    fun getNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, DEFAULT_NOTIFICATIONS)
    fun setNotificationsEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    fun getAutoRetry(): Boolean = prefs.getBoolean(KEY_AUTO_RETRY, DEFAULT_AUTO_RETRY)
    fun setAutoRetry(value: Boolean) = prefs.edit().putBoolean(KEY_AUTO_RETRY, value).apply()

    fun getTheme(): String = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
    fun setTheme(value: String) = prefs.edit().putString(KEY_THEME, value).apply()

    fun getAccentIndex(): Int = prefs.getInt(KEY_ACCENT, DEFAULT_ACCENT)
    fun setAccentIndex(value: Int) = prefs.edit().putInt(KEY_ACCENT, value).apply()

    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    fun setLanguage(value: String) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun getStorageLimitGb(): Int = prefs.getInt(KEY_STORAGE_LIMIT, DEFAULT_STORAGE_LIMIT)
    fun setStorageLimitGb(value: Int) = prefs.edit().putInt(KEY_STORAGE_LIMIT, value).apply()

    fun getBandwidthLimitMbps(): Int = prefs.getInt(KEY_BANDWIDTH_LIMIT, DEFAULT_BANDWIDTH_LIMIT)
    fun setBandwidthLimitMbps(value: Int) = prefs.edit().putInt(KEY_BANDWIDTH_LIMIT, value).apply()

    fun getSmartScheduling(): Boolean = prefs.getBoolean(KEY_SMART_SCHEDULING, DEFAULT_SMART_SCHEDULING)
    fun setSmartScheduling(value: Boolean) = prefs.edit().putBoolean(KEY_SMART_SCHEDULING, value).apply()
}
