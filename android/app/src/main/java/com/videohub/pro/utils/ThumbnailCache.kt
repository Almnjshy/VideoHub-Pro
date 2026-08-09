package com.videohub.pro.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Thumbnail Cache — يحفظ الصور المصغرة محلياً لتسريع العرض.
 *
 * يستخدم LruCache في الذاكرة + تخزين دائم على القرص.
 */
class ThumbnailCache(private val context: Context) {

    private val memoryCache = object : LruCache<String, Bitmap>((12 * 1024 * 1024) / 4) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private val cacheDir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }

    /**
     * Get a thumbnail bitmap from URL.
     * Checks memory cache → disk cache → network.
     */
    suspend fun getThumbnail(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = md5(url)

        // 1. Memory cache
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // 2. Disk cache
        val cacheFile = File(cacheDir, "$cacheKey.jpg")
        if (cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                memoryCache.put(cacheKey, bitmap)
                return@withContext bitmap
            }
        }

        // 3. Network
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connect()

            if (connection.responseCode == 200) {
                val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                if (bitmap != null) {
                    memoryCache.put(cacheKey, bitmap)
                    // Save to disk
                    FileOutputStream(cacheFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    return@withContext bitmap
                }
            }
        } catch (e: Exception) {
            // Network failed — return null
        }

        null
    }

    /**
     * Clear the thumbnail cache (both memory and disk).
     */
    fun clearCache() {
        memoryCache.evictAll()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Get cache size in bytes.
     */
    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
