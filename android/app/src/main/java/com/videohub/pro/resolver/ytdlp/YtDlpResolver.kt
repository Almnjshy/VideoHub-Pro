package com.videohub.pro.resolver.ytdlp

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata
import com.videohub.pro.domain.models.MediaQuality
import com.videohub.pro.domain.models.MediaType
import com.videohub.pro.network.NetworkClient
import com.videohub.pro.resolver.AuthenticationContext
import com.videohub.pro.resolver.ResolveError
import com.videohub.pro.resolver.ResolveResult
import com.videohub.pro.resolver.ResolveStatus
import com.videohub.pro.resolver.ResolverDiagnostics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YtDlpResolver — runs yt-dlp LOCALLY on the device via Chaquopy 17.
 *
 * This is the REAL resolver. No external server needed.
 * yt-dlp is embedded in the APK and runs in a Python 3.13 interpreter
 * on the user's device.
 *
 * Supported platforms: YouTube, TikTok, Facebook, Instagram, Twitter/X,
 * Vimeo, Dailymotion, Reddit, Twitch, SoundCloud, Pinterest, LinkedIn,
 * Tumblr, Streamable, and 1000+ more.
 *
 * Architecture:
 *   Kotlin → Chaquopy JNI → Python 3.13 → yt-dlp → HTTP → platform CDNs
 *
 * The Python script (resolver.py) is bundled in src/main/python/.
 *
 * Authentication: For platforms requiring login (Instagram, LinkedIn, etc.),
 * the AuthenticationManager provides cookies which are passed to yt-dlp via
 * the cookies_json parameter. We do NOT bypass DRM, CAPTCHA, or MFA — if
 * yt-dlp cannot access the content with the provided cookies, it returns
 * AUTHENTICATION_REQUIRED.
 */
@Singleton
class YtDlpResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    @Suppress("unused") private val networkClient: NetworkClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var pythonInitialized = false

    companion object {
        private const val TAG = "YtDlpResolver"
    }

    /**
     * Initialize Python platform (must be called once on app startup).
     * Safe to call multiple times — uses synchronized guard.
     */
    @Synchronized
    fun initialize() {
        if (pythonInitialized) return
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            pythonInitialized = true
            Log.i(TAG, "Python initialized: yt-dlp ${getVersion()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Python", e)
        }
    }

    /**
     * Check if the embedded resolver is ready (Python + yt-dlp available).
     */
    fun isReady(): Boolean {
        return try {
            initialize()
            val py = Python.getInstance()
            val result = py.getModule("resolver").callAttr("health_check").toString()
            val parsed = json.parseToJsonElement(result).jsonObject
            parsed["ok"]?.jsonPrimitive?.contentOrNull == "true"
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            false
        }
    }

    /**
     * Get yt-dlp version string (from embedded Python).
     */
    fun getVersion(): String {
        return try {
            initialize()
            val py = Python.getInstance()
            py.getModule("resolver").callAttr("get_version").toString()
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Check resolver health (for Settings UI).
     */
    suspend fun checkHealth(): ResolverHealth? = withContext(Dispatchers.IO) {
        try {
            val ready = isReady()
            ResolverHealth(
                ok = ready,
                version = if (ready) "17.0.0" else null,
                ytDlpVersion = if (ready) getVersion() else null,
                serviceUrl = "embedded://chaquopy-17",
            )
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed", e)
            null
        }
    }

    /**
     * Resolve a URL to media formats.
     * Uses embedded yt-dlp (Chaquopy) — no external server needed.
     *
     * @param url The media URL to resolve
     * @param taskId Unique task ID for diagnostics
     * @param authContext Optional authentication context (cookies for logged-in platforms)
     */
    suspend fun resolve(
        url: String,
        taskId: String,
        authContext: AuthenticationContext? = null,
    ): ResolveResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        initialize()

        try {
            val py = Python.getInstance()
            val resolverModule = py.getModule("resolver")

            // Convert auth context cookies to JSON for Python.
            // We do NOT bypass DRM/CAPTCHA/MFA — if yt-dlp can't access content
            // with these cookies, it returns AUTHENTICATION_REQUIRED.
            val cookiesJson = authContext?.let {
                json.encodeToString(
                    kotlinx.serialization.serializer<Map<String, String>>(),
                    it.cookies,
                )
            }

            // Call Python resolve(url, cookies_json) — returns JSON string
            val resultJson = resolverModule.callAttr("resolve", url, cookiesJson).toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject

            val ok = parsed["ok"]?.jsonPrimitive?.contentOrNull == "true"

            if (!ok) {
                val error = parsed["error"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                val errorType = parsed["errorType"]?.jsonPrimitive?.contentOrNull
                    ?: "EXTRACTION_FAILED"

                val resolveError = when (errorType) {
                    "AUTHENTICATION_REQUIRED" -> ResolveError.AUTHENTICATION_REQUIRED
                    "UNSUPPORTED_PLATFORM" -> ResolveError.UNSUPPORTED_PLATFORM
                    "NOT_FOUND" -> ResolveError.NO_MEDIA_STREAM
                    "RATE_LIMITED" -> ResolveError.RATE_LIMITED
                    "DRM_PROTECTED" -> ResolveError.UNSUPPORTED_CONTENT_TYPE
                    else -> ResolveError.EXTRACTION_ENGINE_FAILURE
                }

                return@withContext ResolveResult(
                    taskId = taskId,
                    platformId = detectPlatform(url),
                    contentId = null,
                    status = when (resolveError) {
                        ResolveError.AUTHENTICATION_REQUIRED -> ResolveStatus.AUTHENTICATION_REQUIRED
                        ResolveError.UNSUPPORTED_PLATFORM -> ResolveStatus.UNSUPPORTED
                        else -> ResolveStatus.FAILED
                    },
                    error = resolveError,
                    errorMessage = error,
                    diagnostics = ResolverDiagnostics(
                        platform = detectPlatform(url),
                        resolver = "YtDlpResolver",
                        engine = "Chaquopy17/Python3.13",
                        taskId = taskId,
                        stage = "EXTRACT",
                        durationMs = System.currentTimeMillis() - startTime,
                        status = "FAILED",
                        errorCode = errorType,
                    ),
                )
            }

            // Parse metadata + formats
            val metadataObj = parsed["metadata"]?.jsonObject
                ?: return@withContext ResolveResult(
                    taskId = taskId,
                    platformId = detectPlatform(url),
                    contentId = null,
                    status = ResolveStatus.FAILED,
                    error = ResolveError.EXTRACTION_ENGINE_FAILURE,
                    errorMessage = "No metadata returned",
                )

            val formatsArray = metadataObj["formats"]?.jsonArray
                ?: kotlinx.serialization.json.JsonArray(emptyList())
            val formats = formatsArray.map { fmtObj ->
                val fmt = fmtObj.jsonObject
                MediaFormat(
                    id = fmt["id"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                    quality = mapQuality(fmt["quality"]?.jsonPrimitive?.contentOrNull),
                    ext = fmt["ext"]?.jsonPrimitive?.contentOrNull ?: "mp4",
                    sizeBytes = fmt["sizeBytes"]?.jsonPrimitive?.longOrNull ?: 0L,
                    mediaType = mapMediaType(fmt["mediaType"]?.jsonPrimitive?.contentOrNull),
                    hasAudio = fmt["hasAudio"]?.jsonPrimitive?.contentOrNull == "true",
                    bitrate = fmt["bitrate"]?.jsonPrimitive?.intOrNull,
                    fps = fmt["fps"]?.jsonPrimitive?.intOrNull,
                    label = fmt["label"]?.jsonPrimitive?.contentOrNull ?: "Unknown",
                    downloadUrl = fmt["downloadUrl"]?.jsonPrimitive?.contentOrNull,
                )
            }

            val metadata = MediaMetadata(
                title = metadataObj["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled",
                author = metadataObj["author"]?.jsonPrimitive?.contentOrNull,
                thumbnailUrl = metadataObj["thumbnailUrl"]?.jsonPrimitive?.contentOrNull,
                durationSeconds = metadataObj["durationSeconds"]?.jsonPrimitive?.intOrNull,
                description = metadataObj["description"]?.jsonPrimitive?.contentOrNull,
                sourceUrl = metadataObj["sourceUrl"]?.jsonPrimitive?.contentOrNull ?: url,
                platformId = metadataObj["platformId"]?.jsonPrimitive?.contentOrNull
                    ?: detectPlatform(url),
                formats = emptyList(),
                resolvedAt = metadataObj["resolvedAt"]?.jsonPrimitive?.longOrNull
                    ?: System.currentTimeMillis(),
            )

            ResolveResult(
                taskId = taskId,
                platformId = metadata.platformId,
                contentId = null,
                status = ResolveStatus.RESOLVED,
                metadata = metadata,
                formats = formats,
                resolverEngine = "yt-dlp (embedded)",
                diagnostics = ResolverDiagnostics(
                    platform = metadata.platformId,
                    resolver = "YtDlpResolver",
                    engine = "Chaquopy17/Python3.13",
                    taskId = taskId,
                    stage = "EXTRACT",
                    durationMs = System.currentTimeMillis() - startTime,
                    status = "RESOLVED",
                    errorCode = null,
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Resolve failed for $url", e)
            ResolveResult(
                taskId = taskId,
                platformId = detectPlatform(url),
                contentId = null,
                status = ResolveStatus.FAILED,
                error = ResolveError.EXTRACTION_ENGINE_FAILURE,
                errorMessage = e.message ?: "Python execution failed",
                diagnostics = ResolverDiagnostics(
                    platform = detectPlatform(url),
                    resolver = "YtDlpResolver",
                    engine = "Chaquopy17/Python3.13",
                    taskId = taskId,
                    stage = "EXTRACT",
                    durationMs = System.currentTimeMillis() - startTime,
                    status = "FAILED",
                    errorCode = "PYTHON_EXCEPTION",
                ),
            )
        }
    }

    // ============ Helpers ============

    private fun detectPlatform(url: String): String {
        val lower = url.lowercase()
        return when {
            "youtube.com" in lower || "youtu.be" in lower -> "youtube"
            "facebook.com" in lower || "fb.watch" in lower -> "facebook"
            "tiktok.com" in lower -> "tiktok"
            "twitter.com" in lower || "x.com" in lower -> "x"
            "instagram.com" in lower -> "instagram"
            "vimeo.com" in lower -> "vimeo"
            "dailymotion.com" in lower || "dai.ly" in lower -> "dailymotion"
            "reddit.com" in lower -> "reddit"
            "twitch.tv" in lower -> "twitch"
            "soundcloud.com" in lower -> "soundcloud"
            "pinterest.com" in lower || "pin.it" in lower -> "pinterest"
            "linkedin.com" in lower -> "linkedin"
            "tumblr.com" in lower -> "tumblr"
            "streamable.com" in lower -> "streamable"
            else -> "generic"
        }
    }

    private fun mapQuality(q: String?): MediaQuality {
        return when (q) {
            "4k" -> MediaQuality.P4K
            "1080p" -> MediaQuality.P1080
            "720p" -> MediaQuality.P720
            "480p" -> MediaQuality.P480
            "360p" -> MediaQuality.P360
            "240p" -> MediaQuality.P240
            "144p" -> MediaQuality.P144
            "audio" -> MediaQuality.AUDIO
            else -> MediaQuality.P720
        }
    }

    private fun mapMediaType(t: String?): MediaType {
        return when (t) {
            "video" -> MediaType.VIDEO
            "audio" -> MediaType.AUDIO
            "image" -> MediaType.IMAGE
            else -> MediaType.FILE
        }
    }

    // ============ Search & Trending ============

    /**
     * Search YouTube for videos.
     * Returns list of search results.
     */
    suspend fun search(query: String, maxResults: Int = 20): List<SearchResult>? = withContext(Dispatchers.IO) {
        try {
            initialize()
            val py = Python.getInstance()
            val resultJson = py.getModule("resolver").callAttr("search", query, maxResults).toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject
            if (parsed["ok"]?.jsonPrimitive?.contentOrNull != "true") return@withContext null
            val resultsArray = parsed["results"]?.jsonArray ?: return@withContext null
            resultsArray.map { entry ->
                val obj = entry.jsonObject
                SearchResult(
                    title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                    uploader = obj["uploader"]?.jsonPrimitive?.contentOrNull,
                    duration = obj["duration"]?.jsonPrimitive?.intOrNull,
                    viewCount = obj["viewCount"]?.jsonPrimitive?.longOrNull,
                    thumbnail = obj["thumbnail"]?.jsonPrimitive?.contentOrNull,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            null
        }
    }

    /**
     * Get trending videos from YouTube.
     */
    suspend fun getTrending(): List<SearchResult>? = withContext(Dispatchers.IO) {
        try {
            initialize()
            val py = Python.getInstance()
            val resultJson = py.getModule("resolver").callAttr("get_trending").toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject
            if (parsed["ok"]?.jsonPrimitive?.contentOrNull != "true") return@withContext null
            val resultsArray = parsed["results"]?.jsonArray ?: return@withContext null
            resultsArray.map { entry ->
                val obj = entry.jsonObject
                SearchResult(
                    title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                    uploader = obj["uploader"]?.jsonPrimitive?.contentOrNull,
                    duration = obj["duration"]?.jsonPrimitive?.intOrNull,
                    viewCount = obj["viewCount"]?.jsonPrimitive?.longOrNull,
                    thumbnail = obj["thumbnail"]?.jsonPrimitive?.contentOrNull,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Trending failed", e)
            null
        }
    }

    data class SearchResult(
        val title: String,
        val url: String,
        val uploader: String?,
        val duration: Int?,
        val viewCount: Long?,
        val thumbnail: String?,
        val platform: String? = null,
        val uploadDate: String? = null,
    )

    data class VideoInfo(
        val title: String,
        val uploader: String?,
        val uploadDate: String?,
        val duration: Int?,
        val viewCount: Long?,
        val likeCount: Long?,
        val description: String?,
        val thumbnail: String?,
        val categories: List<String>,
        val tags: List<String>,
        val url: String,
        val platform: String,
        val formatsCount: Int,
    )

    // ============ Trending with region ============

    suspend fun getTrendingByCategoryRegion(category: String = "now", region: String = "US"): List<SearchResult>? = withContext(Dispatchers.IO) {
        try {
            initialize()
            val py = Python.getInstance()
            val resultJson = py.getModule("resolver").callAttr("get_trending_by_category_region", category, region, 30).toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject
            if (parsed["ok"]?.jsonPrimitive?.contentOrNull != "true") return@withContext null
            parseSearchResults(parsed)
        } catch (e: Exception) { null }
    }

    // ============ Search with filters ============

    suspend fun searchWithFilters(
        query: String, platform: String = "youtube", maxResults: Int = 15,
        sortBy: String = "relevance", durationFilter: String = "any", timeFilter: String = "any",
    ): List<SearchResult>? = withContext(Dispatchers.IO) {
        try {
            initialize()
            val py = Python.getInstance()
            val resultJson = py.getModule("resolver").callAttr("search_with_filters", query, platform, maxResults, sortBy, durationFilter, timeFilter).toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject
            if (parsed["ok"]?.jsonPrimitive?.contentOrNull != "true") return@withContext null
            parseSearchResults(parsed)
        } catch (e: Exception) { null }
    }

    // ============ Search suggestions ============

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            initialize()
            val py = Python.getInstance()
            val resultJson = py.getModule("resolver").callAttr("get_search_suggestions", query).toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject
            if (parsed["ok"]?.jsonPrimitive?.contentOrNull != "true") return@withContext emptyList()
            parsed["suggestions"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    // ============ Video info for preview ============

    suspend fun getVideoInfo(url: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            initialize()
            val py = Python.getInstance()
            val resultJson = py.getModule("resolver").callAttr("get_video_info", url).toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject
            if (parsed["ok"]?.jsonPrimitive?.contentOrNull != "true") return@withContext null
            VideoInfo(
                title = parsed["title"]?.jsonPrimitive?.contentOrNull ?: "",
                uploader = parsed["uploader"]?.jsonPrimitive?.contentOrNull,
                uploadDate = parsed["upload_date"]?.jsonPrimitive?.contentOrNull,
                duration = parsed["duration"]?.jsonPrimitive?.intOrNull,
                viewCount = parsed["view_count"]?.jsonPrimitive?.longOrNull,
                likeCount = parsed["like_count"]?.jsonPrimitive?.longOrNull,
                description = parsed["description"]?.jsonPrimitive?.contentOrNull,
                thumbnail = parsed["thumbnail"]?.jsonPrimitive?.contentOrNull,
                categories = parsed["categories"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                tags = parsed["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
                url = url,
                platform = parsed["platform"]?.jsonPrimitive?.contentOrNull ?: detectPlatform(url),
                formatsCount = parsed["formats_count"]?.jsonPrimitive?.intOrNull ?: 0,
            )
        } catch (e: Exception) { null }
    }

    private fun parseSearchResults(parsed: JsonObject): List<SearchResult> {
        val resultsArray = parsed["results"]?.jsonArray ?: return emptyList()
        return resultsArray.map { entry ->
            val obj = entry.jsonObject
            SearchResult(
                title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
                url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                uploader = obj["uploader"]?.jsonPrimitive?.contentOrNull,
                duration = obj["duration"]?.jsonPrimitive?.intOrNull,
                viewCount = obj["view_count"]?.jsonPrimitive?.longOrNull ?: obj["viewCount"]?.jsonPrimitive?.longOrNull,
                thumbnail = obj["thumbnail"]?.jsonPrimitive?.contentOrNull,
                platform = obj["platform"]?.jsonPrimitive?.contentOrNull,
                uploadDate = obj["upload_date"]?.jsonPrimitive?.contentOrNull,
            )
        }
    }

    /**
     * Search multiple platforms (YouTube, SoundCloud, YouTube Music).
     */
    suspend fun searchMulti(query: String, platforms: String = "youtube", maxResults: Int = 10): List<SearchResult>? = withContext(Dispatchers.IO) {
        try {
            initialize()
            val py = Python.getInstance()
            val resultJson = py.getModule("resolver").callAttr("search_multi", query, platforms, maxResults).toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject
            if (parsed["ok"]?.jsonPrimitive?.contentOrNull != "true") return@withContext null
            val resultsArray = parsed["results"]?.jsonArray ?: return@withContext null
            resultsArray.map { entry ->
                val obj = entry.jsonObject
                SearchResult(
                    title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                    uploader = obj["uploader"]?.jsonPrimitive?.contentOrNull,
                    duration = obj["duration"]?.jsonPrimitive?.intOrNull,
                    viewCount = obj["viewCount"]?.jsonPrimitive?.longOrNull,
                    thumbnail = obj["thumbnail"]?.jsonPrimitive?.contentOrNull,
                    platform = obj["platform"]?.jsonPrimitive?.contentOrNull,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Multi-search failed", e)
            null
        }
    }

    /**
     * Get trending by category (now, music, gaming, movies, news).
     */
    suspend fun getTrendingByCategory(category: String = "now"): List<SearchResult>? = withContext(Dispatchers.IO) {
        try {
            initialize()
            val py = Python.getInstance()
            val resultJson = py.getModule("resolver").callAttr("get_trending_by_category", category).toString()
            val parsed = json.parseToJsonElement(resultJson).jsonObject
            if (parsed["ok"]?.jsonPrimitive?.contentOrNull != "true") return@withContext null
            val resultsArray = parsed["results"]?.jsonArray ?: return@withContext null
            resultsArray.map { entry ->
                val obj = entry.jsonObject
                SearchResult(
                    title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                    uploader = obj["uploader"]?.jsonPrimitive?.contentOrNull,
                    duration = obj["duration"]?.jsonPrimitive?.intOrNull,
                    viewCount = obj["viewCount"]?.jsonPrimitive?.longOrNull,
                    thumbnail = obj["thumbnail"]?.jsonPrimitive?.contentOrNull,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Trending by category failed", e)
            null
        }
    }

    // ============ Health data class ============

    data class ResolverHealth(
        val ok: Boolean,
        val version: String?,
        val ytDlpVersion: String?,
        val serviceUrl: String,
    )
}
