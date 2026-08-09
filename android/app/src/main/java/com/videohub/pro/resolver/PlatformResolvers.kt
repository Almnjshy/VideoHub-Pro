package com.videohub.pro.resolver

import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata
import com.videohub.pro.domain.models.MediaQuality
import com.videohub.pro.domain.models.MediaType
import com.videohub.pro.network.NetworkClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Platform-specific resolvers — each uses the correct extraction strategy for its platform.
 * No generic HTML scraper. No fake data.
 */

@Singleton
class FacebookResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        return try {
            val html = networkClient.fetchTextWithCookies(request.sourceUrl, request.authenticationContext?.cookies ?: emptyMap())
                ?: return errorResult(request, startTime, "PAGE_FETCH", ResolveError.NETWORK_FAILURE, "Failed to fetch Facebook page")

            // Look for video source in HTML — Facebook embeds direct video URLs for public videos
            val hdUrl = extractFacebookVideoUrl(html, "hd_src")
            val sdUrl = extractFacebookVideoUrl(html, "sd_src")
            val title = extractMetaTag(html, "og:title") ?: "Facebook Video"

            val formats = mutableListOf<MediaFormat>()

            if (hdUrl != null && isValidMediaUrl(hdUrl)) {
                formats.add(MediaFormat(
                    id = "facebook-hd", quality = MediaQuality.P720, ext = "mp4",
                    sizeBytes = 0L, mediaType = MediaType.VIDEO, hasAudio = true,
                    label = "HD · MP4", downloadUrl = hdUrl,
                ))
            }
            if (sdUrl != null && isValidMediaUrl(sdUrl)) {
                formats.add(MediaFormat(
                    id = "facebook-sd", quality = MediaQuality.P480, ext = "mp4",
                    sizeBytes = 0L, mediaType = MediaType.VIDEO, hasAudio = true,
                    label = "SD · MP4", downloadUrl = sdUrl,
                ))
            }

            if (formats.isEmpty()) {
                // Check if auth is needed
                val needsAuth = html.contains("login_form") || html.contains("/login/")
                return if (needsAuth) {
                    errorResult(request, startTime, "AUTH_CHECK", ResolveError.AUTHENTICATION_REQUIRED,
                        "This Facebook video requires login. Please authenticate in Settings.")
                } else {
                    errorResult(request, startTime, "FORMAT_EXTRACTION", ResolveError.NO_MEDIA_STREAM,
                        "No downloadable video URLs found — video may be private or DRM-protected")
                }
            }

            val metadata = MediaMetadata(
                title = title, author = null, thumbnailUrl = extractMetaTag(html, "og:image"),
                durationSeconds = null, description = "Facebook video",
                sourceUrl = request.sourceUrl, platformId = "facebook",
                formats = emptyList(), resolvedAt = System.currentTimeMillis(),
            )

            ResolveResult(
                taskId = request.taskId, platformId = "facebook",
                contentId = request.contentId, status = ResolveStatus.RESOLVED,
                metadata = metadata, formats = formats, resolverEngine = "LocalExtractionEngine",
            )
        } catch (e: Exception) {
            errorResult(request, startTime, "EXCEPTION", ResolveError.EXTRACTION_ENGINE_FAILURE, e.message ?: "Unknown error")
        }
    }

    private fun extractFacebookVideoUrl(html: String, key: String): String? {
        val pattern = Pattern.compile("\"$key\"\\s*:\\s*\"(https?:[^\"]+)\"", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)?.replace("\\/", "/")
        return null
    }

    private fun extractMetaTag(html: String, tag: String): String? {
        val pattern = Pattern.compile("<meta\\s+(?:property|name)=[\"']$tag[\"']\\s+content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun isValidMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("https://") && (lower.contains("fbcdn.net") || lower.contains(".mp4"))
    }

    private fun errorResult(request: ResolveRequest, startTime: Long, stage: String, error: ResolveError, message: String): ResolveResult {
        return ResolveResult(
            taskId = request.taskId, platformId = "facebook", contentId = request.contentId,
            status = ResolveStatus.FAILED, error = error, errorMessage = message,
            diagnostics = ResolverDiagnostics("facebook", "FacebookResolver", "LocalExtractionEngine",
                request.taskId, stage, System.currentTimeMillis() - startTime, "FAILED", error.name),
        )
    }
}

@Singleton
class TikTokResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        return try {
            // TikTok redirects to m.tiktok.com — fetch with cookies
            val html = networkClient.fetchTextWithCookies(request.sourceUrl, request.authenticationContext?.cookies ?: emptyMap())
                ?: return errorResult(request, startTime, "PAGE_FETCH", ResolveError.NETWORK_FAILURE, "Failed to fetch TikTok page")

            // Extract video URL from SIGI_STATE or __NEXT_DATA__
            val videoUrl = extractTikTokVideoUrl(html)
            val title = extractMetaTag(html, "og:title") ?: "TikTok Video"
            val thumbnail = extractMetaTag(html, "og:image")

            if (videoUrl == null || !isValidMediaUrl(videoUrl)) {
                return errorResult(request, startTime, "FORMAT_EXTRACTION", ResolveError.PLATFORM_CHANGED,
                    "TikTok page structure changed — could not extract video URL")
            }

            val formats = listOf(MediaFormat(
                id = "tiktok-direct", quality = MediaQuality.P720, ext = "mp4",
                sizeBytes = 0L, mediaType = MediaType.VIDEO, hasAudio = true,
                label = "MP4 · Direct", downloadUrl = videoUrl,
            ))

            val metadata = MediaMetadata(
                title = title, author = null, thumbnailUrl = thumbnail,
                durationSeconds = null, description = "TikTok video",
                sourceUrl = request.sourceUrl, platformId = "tiktok",
                formats = emptyList(), resolvedAt = System.currentTimeMillis(),
            )

            ResolveResult(
                taskId = request.taskId, platformId = "tiktok", contentId = request.contentId,
                status = ResolveStatus.RESOLVED, metadata = metadata, formats = formats,
                resolverEngine = "LocalExtractionEngine",
            )
        } catch (e: Exception) {
            errorResult(request, startTime, "EXCEPTION", ResolveError.EXTRACTION_ENGINE_FAILURE, e.message ?: "Unknown error")
        }
    }

    private fun extractTikTokVideoUrl(html: String): String? {
        // Look for playAddr in SIGI_STATE or __NEXT_DATA__
        val patterns = listOf(
            Pattern.compile("\"playAddr\"\\s*:\\s*\"(https?:[^\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"downloadAddr\"\\s*:\\s*\"(https?:[^\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"playApi\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                return matcher.group(1)?.replace("\\u002F", "/")?.replace("\\/", "/")
            }
        }
        return null
    }

    private fun extractMetaTag(html: String, tag: String): String? {
        val pattern = Pattern.compile("<meta\\s+(?:property|name)=[\"']$tag[\"']\\s+content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun isValidMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("https://") && (lower.contains("tiktok") || lower.contains("byteoversea") || lower.contains(".mp4"))
    }

    private fun errorResult(request: ResolveRequest, startTime: Long, stage: String, error: ResolveError, message: String): ResolveResult {
        return ResolveResult(
            taskId = request.taskId, platformId = "tiktok", contentId = request.contentId,
            status = ResolveStatus.FAILED, error = error, errorMessage = message,
            diagnostics = ResolverDiagnostics("tiktok", "TikTokResolver", "LocalExtractionEngine",
                request.taskId, stage, System.currentTimeMillis() - startTime, "FAILED", error.name),
        )
    }
}

@Singleton
class InstagramResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        return try {
            // Instagram requires session cookies for most content
            val cookies = request.authenticationContext?.cookies ?: emptyMap()
            if (cookies.isEmpty()) {
                return errorResult(request, startTime, "AUTH_CHECK", ResolveError.AUTHENTICATION_REQUIRED,
                    "Instagram requires login. Please authenticate in Settings.")
            }

            val html = networkClient.fetchTextWithCookies(request.sourceUrl, cookies)
                ?: return errorResult(request, startTime, "PAGE_FETCH", ResolveError.NETWORK_FAILURE, "Failed to fetch Instagram page")

            // Extract video URL from meta tags or JSON
            val videoUrl = extractMetaTag(html, "og:video") ?: extractMetaTag(html, "og:video:secure_url")
            val title = extractMetaTag(html, "og:title") ?: "Instagram Media"
            val thumbnail = extractMetaTag(html, "og:image")

            if (videoUrl == null || !isValidMediaUrl(videoUrl)) {
                // Check if content is private
                val isPrivate = html.contains("private") || html.contains("Page Not Found")
                return if (isPrivate) {
                    errorResult(request, startTime, "PRIVACY_CHECK", ResolveError.PRIVATE_CONTENT,
                        "This Instagram content is private or unavailable")
                } else {
                    errorResult(request, startTime, "FORMAT_EXTRACTION", ResolveError.NO_MEDIA_STREAM,
                        "No downloadable media found — content may be an image or unsupported type")
                }
            }

            val formats = listOf(MediaFormat(
                id = "instagram-direct", quality = MediaQuality.P720, ext = "mp4",
                sizeBytes = 0L, mediaType = MediaType.VIDEO, hasAudio = true,
                label = "MP4 · Direct", downloadUrl = videoUrl,
            ))

            val metadata = MediaMetadata(
                title = title, author = null, thumbnailUrl = thumbnail,
                durationSeconds = null, description = "Instagram media",
                sourceUrl = request.sourceUrl, platformId = "instagram",
                formats = emptyList(), resolvedAt = System.currentTimeMillis(),
            )

            ResolveResult(
                taskId = request.taskId, platformId = "instagram", contentId = request.contentId,
                status = ResolveStatus.RESOLVED, metadata = metadata, formats = formats,
                resolverEngine = "LocalExtractionEngine",
            )
        } catch (e: Exception) {
            errorResult(request, startTime, "EXCEPTION", ResolveError.EXTRACTION_ENGINE_FAILURE, e.message ?: "Unknown error")
        }
    }

    private fun extractMetaTag(html: String, tag: String): String? {
        val pattern = Pattern.compile("<meta\\s+(?:property|name)=[\"']$tag[\"']\\s+content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun isValidMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("https://") && (lower.contains("cdninstagram") || lower.contains("fbcdn") || lower.contains(".mp4"))
    }

    private fun errorResult(request: ResolveRequest, startTime: Long, stage: String, error: ResolveError, message: String): ResolveResult {
        return ResolveResult(
            taskId = request.taskId, platformId = "instagram", contentId = request.contentId,
            status = ResolveStatus.FAILED, error = error, errorMessage = message,
            diagnostics = ResolverDiagnostics("instagram", "InstagramResolver", "LocalExtractionEngine",
                request.taskId, stage, System.currentTimeMillis() - startTime, "FAILED", error.name),
        )
    }
}

@Singleton
class TwitterResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        return try {
            val html = networkClient.fetchTextWithCookies(request.sourceUrl, request.authenticationContext?.cookies ?: emptyMap())
                ?: return errorResult(request, startTime, "PAGE_FETCH", ResolveError.NETWORK_FAILURE, "Failed to fetch X/Twitter page")

            // Look for video variant URLs in the tweet data
            val videoUrl = extractTwitterVideoUrl(html)
            val title = extractMetaTag(html, "og:title") ?: "X/Twitter Video"
            val thumbnail = extractMetaTag(html, "og:image")

            if (videoUrl == null || !isValidMediaUrl(videoUrl)) {
                return errorResult(request, startTime, "FORMAT_EXTRACTION", ResolveError.NO_MEDIA_STREAM,
                    "No downloadable video found — tweet may not contain video or may require authentication")
            }

            val formats = listOf(MediaFormat(
                id = "twitter-direct", quality = MediaQuality.P720, ext = "mp4",
                sizeBytes = 0L, mediaType = MediaType.VIDEO, hasAudio = true,
                label = "MP4 · Direct", downloadUrl = videoUrl,
            ))

            val metadata = MediaMetadata(
                title = title, author = null, thumbnailUrl = thumbnail,
                durationSeconds = null, description = "X/Twitter video",
                sourceUrl = request.sourceUrl, platformId = "x",
                formats = emptyList(), resolvedAt = System.currentTimeMillis(),
            )

            ResolveResult(
                taskId = request.taskId, platformId = "x", contentId = request.contentId,
                status = ResolveStatus.RESOLVED, metadata = metadata, formats = formats,
                resolverEngine = "LocalExtractionEngine",
            )
        } catch (e: Exception) {
            errorResult(request, startTime, "EXCEPTION", ResolveError.EXTRACTION_ENGINE_FAILURE, e.message ?: "Unknown error")
        }
    }

    private fun extractTwitterVideoUrl(html: String): String? {
        val patterns = listOf(
            Pattern.compile("\"video_url\"\\s*:\\s*\"(https?:[^\"]+\\.mp4[^\"]*)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"url\"\\s*:\\s*\"(https://video[^\"}]+\\.mp4[^\"]*)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("og:video\"\\s+content=\"(https://[^\"]+\\.mp4[^\"]*)\"", Pattern.CASE_INSENSITIVE),
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) return matcher.group(1)?.replace("\\/", "/")
        }
        return null
    }

    private fun extractMetaTag(html: String, tag: String): String? {
        val pattern = Pattern.compile("<meta\\s+(?:property|name)=[\"']$tag[\"']\\s+content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    private fun isValidMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("https://") && (lower.contains("video.twimg") || lower.contains(".mp4"))
    }

    private fun errorResult(request: ResolveRequest, startTime: Long, stage: String, error: ResolveError, message: String): ResolveResult {
        return ResolveResult(
            taskId = request.taskId, platformId = "x", contentId = request.contentId,
            status = ResolveStatus.FAILED, error = error, errorMessage = message,
            diagnostics = ResolverDiagnostics("x", "TwitterResolver", "LocalExtractionEngine",
                request.taskId, stage, System.currentTimeMillis() - startTime, "FAILED", error.name),
        )
    }
}

@Singleton
class RedditResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun resolve(request: ResolveRequest): ResolveResult {
        val startTime = System.currentTimeMillis()
        return try {
            // Reddit has a public JSON API — append .json to the URL
            val jsonUrl = request.sourceUrl.trimEnd('/') + ".json"
            val jsonStr = networkClient.fetchJson(jsonUrl)
                ?: return errorResult(request, startTime, "API_FETCH", ResolveError.NETWORK_FAILURE, "Failed to fetch Reddit data")

            val json = JSONArray(jsonStr)
            val postData = json.optJSONObject(0)?.optJSONObject("data")
                ?.optJSONArray("children")?.optJSONObject(0)?.optJSONObject("data")
                ?: return errorResult(request, startTime, "PARSE", ResolveError.NO_MEDIA_STREAM, "Could not parse Reddit post data")

            val title = postData.optString("title", "Reddit Post")
            val author = postData.optString("author", "")

            // Check for Reddit video
            val media = postData.optJSONObject("media")
            val redditVideo = media?.optJSONObject("reddit_video")

            if (redditVideo != null) {
                val hlsUrl = redditVideo.optString("hls_url", "").ifEmpty { null }
                val dashUrl = redditVideo.optString("dash_url", "").ifEmpty { null }
                val fallbackUrl = redditVideo.optString("fallback_url", "").ifEmpty { null }

                // fallback_url is usually a direct MP4
                if (fallbackUrl != null && isValidMediaUrl(fallbackUrl)) {
                    val formats = listOf(MediaFormat(
                        id = "reddit-video", quality = MediaQuality.P720, ext = "mp4",
                        sizeBytes = 0L, mediaType = MediaType.VIDEO, hasAudio = true,
                        label = "Reddit Video · MP4", downloadUrl = fallbackUrl,
                    ))

                    val metadata = MediaMetadata(
                        title = title, author = author.ifEmpty { null },
                        thumbnailUrl = postData.optString("thumbnail", "").ifEmpty { null },
                        durationSeconds = redditVideo.optInt("duration", 0).takeIf { it > 0 },
                        description = "Reddit video", sourceUrl = request.sourceUrl,
                        platformId = "reddit", formats = emptyList(), resolvedAt = System.currentTimeMillis(),
                    )

                    return ResolveResult(
                        taskId = request.taskId, platformId = "reddit", contentId = request.contentId,
                        status = ResolveStatus.RESOLVED, metadata = metadata, formats = formats,
                        resolverEngine = "LocalExtractionEngine",
                    )
                }
            }

            // Check for direct image
            val url = postData.optString("url", "")
            if (url.isNotEmpty() && (url.contains(".jpg") || url.contains(".png") || url.contains(".gif"))) {
                val formats = listOf(MediaFormat(
                    id = "reddit-image", quality = MediaQuality.AUDIO, ext = url.substringAfterLast(".", "jpg"),
                    sizeBytes = 0L, mediaType = MediaType.IMAGE, hasAudio = false,
                    label = "Image · ${url.substringAfterLast(".", "jpg")}", downloadUrl = url,
                ))

                val metadata = MediaMetadata(
                    title = title, author = author.ifEmpty { null },
                    thumbnailUrl = null, durationSeconds = null,
                    description = "Reddit image", sourceUrl = request.sourceUrl,
                    platformId = "reddit", formats = emptyList(), resolvedAt = System.currentTimeMillis(),
                )

                return ResolveResult(
                    taskId = request.taskId, platformId = "reddit", contentId = request.contentId,
                    status = ResolveStatus.RESOLVED, metadata = metadata, formats = formats,
                    resolverEngine = "LocalExtractionEngine",
                )
            }

            errorResult(request, startTime, "MEDIA_CHECK", ResolveError.NO_MEDIA_STREAM,
                "No downloadable media found — post may contain external link or text only")
        } catch (e: Exception) {
            errorResult(request, startTime, "EXCEPTION", ResolveError.EXTRACTION_ENGINE_FAILURE, e.message ?: "Unknown error")
        }
    }

    private fun isValidMediaUrl(url: String): Boolean {
        return url.startsWith("https://") && (url.contains("redditmedia") || url.contains("reddit.com") || url.contains(".mp4"))
    }

    private fun errorResult(request: ResolveRequest, startTime: Long, stage: String, error: ResolveError, message: String): ResolveResult {
        return ResolveResult(
            taskId = request.taskId, platformId = "reddit", contentId = request.contentId,
            status = ResolveStatus.FAILED, error = error, errorMessage = message,
            diagnostics = ResolverDiagnostics("reddit", "RedditResolver", "LocalExtractionEngine",
                request.taskId, stage, System.currentTimeMillis() - startTime, "FAILED", error.name),
        )
    }
}
