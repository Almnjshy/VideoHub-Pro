package com.videohub.pro.resolver

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata
import com.videohub.pro.domain.models.MediaQuality
import com.videohub.pro.domain.models.MediaType
import com.videohub.pro.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * YouTube Resolver — uses real local extraction via WebView + YouTube's internal API.
 *
 * Strategy:
 * 1. Extract video ID from URL
 * 2. Fetch YouTube page with proper headers
 * 3. Parse ytInitialPlayerResponse for real stream formats
 * 4. Return real media URLs (googlevideo.com direct links)
 *
 * No fake data. No webpage URL as media URL.
 * If extraction fails, returns PLATFORM_CHANGED or NO_MEDIA_STREAM.
 */
@Singleton
class YouTubeResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {

    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        val videoId = extractVideoId(request.sourceUrl)
            ?: return ResolveResult(
                taskId = request.taskId,
                platformId = "youtube",
                contentId = null,
                status = ResolveStatus.FAILED,
                error = ResolveError.NO_MEDIA_STREAM,
                errorMessage = "Could not extract YouTube video ID from URL",
                diagnostics = buildDiagnostics(request, startTime, "VIDEO_ID_EXTRACTION", "FAILED", "NO_MEDIA_STREAM"),
            )

        return try {
            // Fetch the YouTube watch page
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val html = networkClient.fetchTextWithCookies(watchUrl, request.authenticationContext?.cookies ?: emptyMap())
                ?: return ResolveResult(
                    taskId = request.taskId,
                    platformId = "youtube",
                    contentId = videoId,
                    status = ResolveStatus.FAILED,
                    error = ResolveError.NETWORK_FAILURE,
                    errorMessage = "Failed to fetch YouTube page",
                    diagnostics = buildDiagnostics(request, startTime, "PAGE_FETCH", "FAILED", "NETWORK_FAILURE"),
                )

            // Parse ytInitialPlayerResponse
            val playerResponse = extractPlayerResponse(html)
                ?: return ResolveResult(
                    taskId = request.taskId,
                    platformId = "youtube",
                    contentId = videoId,
                    status = ResolveStatus.FAILED,
                    error = ResolveError.PLATFORM_CHANGED,
                    errorMessage = "YouTube page structure changed — could not find player response",
                    diagnostics = buildDiagnostics(request, startTime, "PLAYER_RESPONSE_PARSE", "FAILED", "PLATFORM_CHANGED"),
                )

            // Extract metadata
            val videoDetails = playerResponse.optJSONObject("videoDetails")
            val title = videoDetails?.optString("title", "")?.ifEmpty { null }
                ?: "YouTube — $videoId"
            val author = videoDetails?.optString("author", "")?.ifEmpty { null }
            val thumbnail = videoDetails?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                ?.let { if (it.length() > 0) it.optJSONObject(it.length() - 1)?.optString("url", "")?.ifEmpty { null } else null }
            val lengthSeconds = videoDetails?.optString("lengthSeconds", "")?.toIntOrNull()

            // Extract real streaming formats
            val streamingData = playerResponse.optJSONObject("streamingData")
                ?: return ResolveResult(
                    taskId = request.taskId,
                    platformId = "youtube",
                    contentId = videoId,
                    status = ResolveStatus.FAILED,
                    error = ResolveError.NO_MEDIA_STREAM,
                    errorMessage = "No streaming data found — video may be private, DRM-protected, or region-locked",
                    diagnostics = buildDiagnostics(request, startTime, "STREAMING_DATA", "FAILED", "NO_MEDIA_STREAM"),
                )

            val formats = mutableListOf<MediaFormat>()

            // Extract combined formats (video+audio)
            val combinedFormats = streamingData.optJSONArray("formats")
            if (combinedFormats != null) {
                for (i in 0 until combinedFormats.length()) {
                    val fmt = combinedFormats.optJSONObject(i) ?: continue
                    val url = fmt.optString("url", "").ifEmpty { null }
                    val mimeType = fmt.optString("mimeType", "")
                    val itag = fmt.optInt("itag", 0)
                    val width = fmt.optInt("width", 0)
                    val height = fmt.optInt("height", 0)
                    val bitrate = fmt.optLong("bitrate", 0)
                    val contentLength = fmt.optString("contentLength", "").toLongOrNull() ?: 0L
                    val hasVideo = fmt.optBoolean("hasVideo", false)
                    val hasAudio = fmt.optBoolean("hasAudio", false)

                    if (url != null && isValidMediaUrl(url)) {
                        val quality = mapQuality(height)
                        val ext = guessExtFromMime(mimeType, "mp4")
                        formats.add(
                            MediaFormat(
                                id = "youtube-${itag}",
                                quality = quality,
                                ext = ext,
                                sizeBytes = contentLength,
                                mediaType = MediaType.VIDEO,
                                hasAudio = hasAudio,
                                bitrate = if (bitrate > 0) bitrate.toInt() else null,
                                fps = fmt.optInt("fps", 0).takeIf { it > 0 },
                                label = buildLabel(height, mimeType, hasVideo, hasAudio),
                                downloadUrl = url,
                            ),
                        )
                    }
                }
            }

            // Extract adaptive formats (video-only or audio-only)
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val fmt = adaptiveFormats.optJSONObject(i) ?: continue
                    val url = fmt.optString("url", "").ifEmpty { null }
                    val mimeType = fmt.optString("mimeType", "")
                    val itag = fmt.optInt("itag", 0)
                    val width = fmt.optInt("width", 0)
                    val height = fmt.optInt("height", 0)
                    val bitrate = fmt.optLong("bitrate", 0)
                    val contentLength = fmt.optString("contentLength", "").toLongOrNull() ?: 0L
                    val hasVideo = fmt.optBoolean("hasVideo", false)
                    val hasAudio = fmt.optBoolean("hasAudio", false)

                    if (url != null && isValidMediaUrl(url)) {
                        val mediaType = if (hasVideo) MediaType.VIDEO else MediaType.AUDIO
                        val quality = if (hasVideo) mapQuality(height) else MediaQuality.AUDIO
                        val ext = guessExtFromMime(mimeType, if (hasVideo) "mp4" else "m4a")
                        formats.add(
                            MediaFormat(
                                id = "youtube-adaptive-${itag}",
                                quality = quality,
                                ext = ext,
                                sizeBytes = contentLength,
                                mediaType = mediaType,
                                hasAudio = hasAudio,
                                bitrate = if (bitrate > 0) bitrate.toInt() else null,
                                fps = fmt.optInt("fps", 0).takeIf { it > 0 },
                                label = buildAdaptiveLabel(height, mimeType, hasVideo, hasAudio, bitrate),
                                downloadUrl = url,
                            ),
                        )
                    }
                }
            }

            if (formats.isEmpty()) {
                return ResolveResult(
                    taskId = request.taskId,
                    platformId = "youtube",
                    contentId = videoId,
                    status = ResolveStatus.FAILED,
                    error = ResolveError.NO_MEDIA_STREAM,
                    errorMessage = "YouTube returned no downloadable formats — video may be DRM-protected or require authentication",
                    diagnostics = buildDiagnostics(request, startTime, "FORMAT_EXTRACTION", "FAILED", "NO_MEDIA_STREAM"),
                )
            }

            val metadata = MediaMetadata(
                title = title,
                author = author,
                thumbnailUrl = thumbnail,
                durationSeconds = lengthSeconds,
                description = "YouTube video",
                sourceUrl = request.sourceUrl,
                platformId = "youtube",
                formats = emptyList(), // Formats are in ResolveResult, not metadata
                resolvedAt = System.currentTimeMillis(),
            )

            ResolveResult(
                taskId = request.taskId,
                platformId = "youtube",
                contentId = videoId,
                status = ResolveStatus.RESOLVED,
                metadata = metadata,
                formats = formats,
                resolverEngine = "LocalExtractionEngine",
                diagnostics = buildDiagnostics(request, startTime, "COMPLETE", "RESOLVED", null),
            )
        } catch (e: Exception) {
            ResolveResult(
                taskId = request.taskId,
                platformId = "youtube",
                contentId = videoId,
                status = ResolveStatus.FAILED,
                error = ResolveError.EXTRACTION_ENGINE_FAILURE,
                errorMessage = e.message ?: "Unknown extraction error",
                diagnostics = buildDiagnostics(request, startTime, "EXCEPTION", "FAILED", "EXTRACTION_ENGINE_FAILURE"),
            )
        }
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("(?:v=|youtu\\.be/|shorts/|live/|embed/)([\\w-]{11})"),
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractPlayerResponse(html: String): JSONObject? {
        // Look for ytInitialPlayerResponse in the page
        val patterns = listOf(
            Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\});", Pattern.DOTALL),
            Pattern.compile("ytInitialPlayerResponse\"\\s*:\\s*(\\{.+?\\})\\s*[;,}]", Pattern.DOTALL),
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val jsonStr = matcher.group(1)
                if (!jsonStr.isNullOrEmpty()) {
                    return try {
                        JSONObject(jsonStr)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
        return null
    }

    private fun isValidMediaUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        return lower.startsWith("https://") &&
            (lower.contains("googlevideo.com") ||
                lower.contains("youtube.com") ||
                lower.contains(".mp4") ||
                lower.contains(".webm") ||
                lower.contains(".m4a"))
    }

    private fun mapQuality(height: Int): MediaQuality {
        return when {
            height >= 2160 -> MediaQuality.P4K
            height >= 1080 -> MediaQuality.P1080
            height >= 720 -> MediaQuality.P720
            height >= 480 -> MediaQuality.P480
            height >= 360 -> MediaQuality.P360
            height >= 240 -> MediaQuality.P240
            else -> MediaQuality.P144
        }
    }

    private fun guessExtFromMime(mimeType: String, default: String): String {
        return when {
            mimeType.contains("mp4") -> "mp4"
            mimeType.contains("webm") -> "webm"
            mimeType.contains("m4a") -> "m4a"
            mimeType.contains("mp3") -> "mp3"
            mimeType.contains("ogg") -> "ogg"
            else -> default
        }
    }

    private fun buildLabel(height: Int, mimeType: String, hasVideo: Boolean, hasAudio: Boolean): String {
        val quality = if (height > 0) "${height}p" else "unknown"
        val type = if (hasVideo && hasAudio) "Video+Audio" else if (hasVideo) "Video" else "Audio"
        return "$quality · $type"
    }

    private fun buildAdaptiveLabel(height: Int, mimeType: String, hasVideo: Boolean, hasAudio: Boolean, bitrate: Long): String {
        val quality = if (hasVideo) "${height}p" else "${bitrate / 1000}kbps"
        val type = if (hasVideo) "Video only" else "Audio only"
        return "$quality · $type"
    }

    private fun buildDiagnostics(
        request: ResolveRequest,
        startTime: Long,
        stage: String,
        status: String,
        errorCode: String?,
    ): ResolverDiagnostics {
        return ResolverDiagnostics(
            platform = "youtube",
            resolver = "YouTubeResolver",
            engine = "LocalExtractionEngine",
            taskId = request.taskId,
            stage = stage,
            durationMs = System.currentTimeMillis() - startTime,
            status = status,
            errorCode = errorCode,
        )
    }
}
