package com.videohub.pro.resolver

import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata
import com.videohub.pro.network.NetworkClient
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Additional platform resolvers — Twitch, SoundCloud, Pinterest, LinkedIn, Tumblr.
 * Each uses the correct extraction strategy. No fake data.
 */

@Singleton
class TwitchResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        // Twitch VODs require specialized extraction (streaming protocol, not direct MP4)
        // Report BACKEND_REQUIRED honestly
        return ResolveResult(
            taskId = request.taskId, platformId = "twitch", contentId = request.contentId,
            status = ResolveStatus.BACKEND_REQUIRED,
            error = ResolveError.BACKEND_UNAVAILABLE,
            errorMessage = "Twitch requires specialized extraction engine (HLS stream processing). Not available locally.",
            diagnostics = ResolverDiagnostics("twitch", "TwitchResolver", "N/A",
                request.taskId, "CAPABILITY_CHECK", System.currentTimeMillis() - startTime, "BACKEND_REQUIRED", "BACKEND_UNAVAILABLE"),
        )
    }
}

@Singleton
class SoundCloudResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        return try {
            // SoundCloud oEmbed gives metadata
            val oembedUrl = "https://soundcloud.com/oembed?format=json&url=${java.net.URLEncoder.encode(request.sourceUrl, "UTF-8")}"
            val oembedJson = networkClient.fetchJson(oembedUrl)
                ?: return errorResult(request, startTime, "OEMBED_FETCH", ResolveError.NETWORK_FAILURE, "Failed to fetch SoundCloud metadata")

            val json = JSONObject(oembedJson)
            val title = json.optString("title", "").ifEmpty { "SoundCloud Track" }
            val author = json.optString("author_name", "").ifEmpty { null }
            val thumbnail = json.optString("thumbnail_url", "").ifEmpty { null }

            // SoundCloud requires API key for direct stream URLs
            // Without CLIENT_ID, we cannot extract the actual media URL
            ResolveResult(
                taskId = request.taskId, platformId = "soundcloud", contentId = request.contentId,
                status = ResolveStatus.BACKEND_REQUIRED,
                error = ResolveError.BACKEND_UNAVAILABLE,
                errorMessage = "SoundCloud requires API key (CLIENT_ID) for stream URL extraction. Not available locally.",
                metadata = MediaMetadata(
                    title = title, author = author, thumbnailUrl = thumbnail,
                    durationSeconds = null, description = "SoundCloud track",
                    sourceUrl = request.sourceUrl, platformId = "soundcloud",
                    formats = emptyList(), resolvedAt = System.currentTimeMillis(),
                ),
                diagnostics = ResolverDiagnostics("soundcloud", "SoundCloudResolver", "N/A",
                    request.taskId, "API_KEY_CHECK", System.currentTimeMillis() - startTime, "BACKEND_REQUIRED", "BACKEND_UNAVAILABLE"),
            )
        } catch (e: Exception) {
            errorResult(request, startTime, "EXCEPTION", ResolveError.EXTRACTION_ENGINE_FAILURE, e.message ?: "Unknown error")
        }
    }

    private fun errorResult(request: ResolveRequest, startTime: Long, stage: String, error: ResolveError, message: String): ResolveResult {
        return ResolveResult(
            taskId = request.taskId, platformId = "soundcloud", contentId = request.contentId,
            status = ResolveStatus.FAILED, error = error, errorMessage = message,
            diagnostics = ResolverDiagnostics("soundcloud", "SoundCloudResolver", "N/A",
                request.taskId, stage, System.currentTimeMillis() - startTime, "FAILED", error.name),
        )
    }
}

@Singleton
class PinterestResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        return try {
            val html = networkClient.fetchText(request.sourceUrl)
                ?: return errorResult(request, startTime, "PAGE_FETCH", ResolveError.NETWORK_FAILURE, "Failed to fetch Pinterest page")

            // Pinterest embeds the actual image/video URL in og:image or og:video
            val imageUrl = extractMetaTag(html, "og:image")
            val videoUrl = extractMetaTag(html, "og:video")
            val title = extractMetaTag(html, "og:title") ?: "Pinterest Pin"

            val formats = mutableListOf<MediaFormat>()

            if (videoUrl != null && isValidMediaUrl(videoUrl)) {
                formats.add(MediaFormat(
                    id = "pinterest-video", quality = com.videohub.pro.domain.models.MediaQuality.P720,
                    ext = "mp4", sizeBytes = 0L, mediaType = com.videohub.pro.domain.models.MediaType.VIDEO,
                    hasAudio = true, label = "Video · MP4", downloadUrl = videoUrl,
                ))
            } else if (imageUrl != null && isValidImageUrl(imageUrl)) {
                val ext = imageUrl.substringAfterLast(".", "jpg").take(4)
                formats.add(MediaFormat(
                    id = "pinterest-image", quality = com.videohub.pro.domain.models.MediaQuality.AUDIO,
                    ext = ext, sizeBytes = 0L, mediaType = com.videohub.pro.domain.models.MediaType.IMAGE,
                    hasAudio = false, label = "Image · ${ext.uppercase()}", downloadUrl = imageUrl,
                ))
            }

            if (formats.isEmpty()) {
                return errorResult(request, startTime, "FORMAT_EXTRACTION", ResolveError.NO_MEDIA_STREAM,
                    "No downloadable media found on Pinterest page")
            }

            val metadata = MediaMetadata(
                title = title, author = null, thumbnailUrl = imageUrl,
                durationSeconds = null, description = "Pinterest pin",
                sourceUrl = request.sourceUrl, platformId = "pinterest",
                formats = emptyList(), resolvedAt = System.currentTimeMillis(),
            )

            ResolveResult(
                taskId = request.taskId, platformId = "pinterest", contentId = request.contentId,
                status = ResolveStatus.RESOLVED, metadata = metadata, formats = formats,
                resolverEngine = "LocalExtractionEngine",
            )
        } catch (e: Exception) {
            errorResult(request, startTime, "EXCEPTION", ResolveError.EXTRACTION_ENGINE_FAILURE, e.message ?: "Unknown error")
        }
    }

    private fun extractMetaTag(html: String, tag: String): String? {
        val pattern = java.util.regex.Pattern.compile("<meta\\s+(?:property|name)=[\"']$tag[\"']\\s+content=[\"']([^\"']*)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun isValidMediaUrl(url: String): Boolean {
        return url.startsWith("https://") && (url.contains(".mp4") || url.contains("pinimg"))
    }

    private fun isValidImageUrl(url: String): Boolean {
        return url.startsWith("https://") && (url.contains("pinimg") || url.contains(".jpg") || url.contains(".png") || url.contains(".gif"))
    }

    private fun errorResult(request: ResolveRequest, startTime: Long, stage: String, error: ResolveError, message: String): ResolveResult {
        return ResolveResult(
            taskId = request.taskId, platformId = "pinterest", contentId = request.contentId,
            status = ResolveStatus.FAILED, error = error, errorMessage = message,
            diagnostics = ResolverDiagnostics("pinterest", "PinterestResolver", "LocalExtractionEngine",
                request.taskId, stage, System.currentTimeMillis() - startTime, "FAILED", error.name),
        )
    }
}

@Singleton
class LinkedInResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        // LinkedIn requires authentication for all media access
        return ResolveResult(
            taskId = request.taskId, platformId = "linkedin", contentId = request.contentId,
            status = ResolveStatus.FAILED,
            error = ResolveError.AUTHENTICATION_REQUIRED,
            errorMessage = "LinkedIn requires authentication. Please log in via Settings.",
            diagnostics = ResolverDiagnostics("linkedin", "LinkedInResolver", "N/A",
                request.taskId, "AUTH_CHECK", System.currentTimeMillis() - startTime, "FAILED", "AUTHENTICATION_REQUIRED"),
        )
    }
}

@Singleton
class TumblrResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        return try {
            val html = networkClient.fetchText(request.sourceUrl)
                ?: return errorResult(request, startTime, "PAGE_FETCH", ResolveError.NETWORK_FAILURE, "Failed to fetch Tumblr page")

            // Tumblr posts often have direct media URLs in the page
            val videoUrl = extractMetaTag(html, "og:video")
            val imageUrl = extractMetaTag(html, "og:image")
            val title = extractMetaTag(html, "og:title") ?: "Tumblr Post"

            val formats = mutableListOf<MediaFormat>()

            if (videoUrl != null && videoUrl.contains(".mp4")) {
                formats.add(MediaFormat(
                    id = "tumblr-video", quality = com.videohub.pro.domain.models.MediaQuality.P720,
                    ext = "mp4", sizeBytes = 0L, mediaType = com.videohub.pro.domain.models.MediaType.VIDEO,
                    hasAudio = true, label = "Video · MP4", downloadUrl = videoUrl,
                ))
            } else if (imageUrl != null && imageUrl.startsWith("https://")) {
                val ext = imageUrl.substringAfterLast(".", "jpg").take(4)
                formats.add(MediaFormat(
                    id = "tumblr-image", quality = com.videohub.pro.domain.models.MediaQuality.AUDIO,
                    ext = ext, sizeBytes = 0L, mediaType = com.videohub.pro.domain.models.MediaType.IMAGE,
                    hasAudio = false, label = "Image · ${ext.uppercase()}", downloadUrl = imageUrl,
                ))
            }

            if (formats.isEmpty()) {
                return errorResult(request, startTime, "FORMAT_EXTRACTION", ResolveError.NO_MEDIA_STREAM,
                    "No downloadable media found — post may be text-only or require API key")
            }

            val metadata = MediaMetadata(
                title = title, author = null, thumbnailUrl = imageUrl,
                durationSeconds = null, description = "Tumblr post",
                sourceUrl = request.sourceUrl, platformId = "tumblr",
                formats = emptyList(), resolvedAt = System.currentTimeMillis(),
            )

            ResolveResult(
                taskId = request.taskId, platformId = "tumblr", contentId = request.contentId,
                status = ResolveStatus.RESOLVED, metadata = metadata, formats = formats,
                resolverEngine = "LocalExtractionEngine",
            )
        } catch (e: Exception) {
            errorResult(request, startTime, "EXCEPTION", ResolveError.EXTRACTION_ENGINE_FAILURE, e.message ?: "Unknown error")
        }
    }

    private fun extractMetaTag(html: String, tag: String): String? {
        val pattern = java.util.regex.Pattern.compile("<meta\\s+(?:property|name)=[\"']$tag[\"']\\s+content=[\"']([^\"']*)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun errorResult(request: ResolveRequest, startTime: Long, stage: String, error: ResolveError, message: String): ResolveResult {
        return ResolveResult(
            taskId = request.taskId, platformId = "tumblr", contentId = request.contentId,
            status = ResolveStatus.FAILED, error = error, errorMessage = message,
            diagnostics = ResolverDiagnostics("tumblr", "TumblrResolver", "LocalExtractionEngine",
                request.taskId, stage, System.currentTimeMillis() - startTime, "FAILED", error.name),
        )
    }
}
