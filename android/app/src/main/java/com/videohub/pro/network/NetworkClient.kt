package com.videohub.pro.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real HTTP networking client using OkHttp.
 * Handles all internet communication for the app — metadata fetching AND real file downloads.
 */
@Singleton
class NetworkClient @Inject constructor() {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Fetch raw text content from a URL (HTTP GET).
     * Returns null on failure.
     */
    fun fetchText(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/json,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetch JSON from a URL.
     * Returns null on failure.
     */
    fun fetchJson(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetch text from URL with cookies (for authenticated requests).
     */
    fun fetchTextWithCookies(url: String, cookies: Map<String, String>): String? {
        return try {
            val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/json,application/xhtml+xml")
                .header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
                .header("Cookie", cookieHeader)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if the device has internet connectivity.
     */
    fun isOnline(): Boolean {
        return try {
            val request = Request.Builder()
                .url("https://www.google.com/generate_204")
                .head()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 204
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Download a file from a URL to a local path — REAL bytes, REAL progress.
     *
     * @param url The actual media download URL
     * @param outputFile The local file to write bytes to
     * @param onProgress Callback receiving (downloadedBytes, totalBytes) — called with REAL numbers
     * @return DownloadResult with success/failure and file size
     */
    fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): DownloadResult {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header("Referer", extractReferer(url))
                .build()

            client.newCall(request).execute().use { response: Response ->
                if (!response.isSuccessful) {
                    return DownloadResult.Error("HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: return DownloadResult.Error("Empty response body")

                // CRITICAL: Validate Content-Type is actually media, not HTML
                val contentType = body.contentType()?.toString()?.lowercase() ?: ""
                val isMediaContent = contentType.contains("video/") ||
                    contentType.contains("audio/") ||
                    contentType.contains("application/octet-stream") ||
                    contentType.contains("binary/") ||
                    contentType.isBlank() // Some CDNs don't send Content-Type

                if (!isMediaContent) {
                    return DownloadResult.Error("Response is not a media file (Content-Type: $contentType). URL may point to a webpage.")
                }

                val totalBytes = body.contentLength()
                val inputStream: InputStream = body.byteStream()

                outputFile.parentFile?.mkdirs()

                var downloadedBytes = 0L
                val buffer = ByteArray(8192)
                var lastProgressReport = 0L

                FileOutputStream(outputFile).use { output ->
                    while (true) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break

                        output.write(buffer, 0, read)
                        downloadedBytes += read.toLong()

                        if (downloadedBytes - lastProgressReport >= 100_000 || (totalBytes > 0 && downloadedBytes >= totalBytes)) {
                            onProgress(downloadedBytes, if (totalBytes > 0) totalBytes else -1)
                            lastProgressReport = downloadedBytes
                        }
                    }
                    output.flush()
                }

                if (!outputFile.exists()) {
                    return DownloadResult.Error("File was not created")
                }

                val fileSize = outputFile.length()
                if (fileSize == 0L) {
                    outputFile.delete()
                    return DownloadResult.Error("Downloaded file is empty (0 bytes)")
                }

                onProgress(fileSize, fileSize)

                DownloadResult.Success(filePath = outputFile.absolutePath, fileSize = fileSize)
            }
        } catch (e: Exception) {
            if (outputFile.exists()) {
                outputFile.delete()
            }
            DownloadResult.Error(e.message ?: "Unknown download error")
        }
    }

    private fun extractReferer(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}/"
        } catch (e: Exception) {
            url
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}

sealed class DownloadResult {
    data class Success(val filePath: String, val fileSize: Long) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}
