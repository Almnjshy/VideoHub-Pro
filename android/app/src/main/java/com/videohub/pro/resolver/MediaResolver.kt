package com.videohub.pro.resolver

import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata
import com.videohub.pro.domain.models.MediaQuality
import com.videohub.pro.domain.models.MediaType
import com.videohub.pro.network.NetworkClient
import com.videohub.pro.plugins.PlatformPlugin
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a media resolution attempt.
 */
sealed class ResolutionResult {
    data class Success(val metadata: MediaMetadata, val formats: List<MediaFormat>) : ResolutionResult()
    data class Error(val reason: ResolutionError, val message: String) : ResolutionResult()
}

enum class ResolutionError {
    UNSUPPORTED_PLATFORM,
    AUTH_REQUIRED,
    PRIVATE_CONTENT,
    NO_MEDIA_FOUND,
    NETWORK_FAILURE,
    PLATFORM_CHANGED,
    BACKEND_UNAVAILABLE,
}

/**
 * Media Resolution Engine — the REAL extraction pipeline.
 *
 * Architecture:
 *   URL → Platform Detection → Platform-specific Resolver → Real Media Formats → Verified URLs
 *
 * Each platform has its own resolver method that uses the appropriate
 * extraction strategy for that platform. No generic HTML scraping
 * that pretends to work for everything.
 *
 * Platforms that cannot be resolved are reported as UNSUPPORTED.
 */
@Singleton
class MediaResolver @Inject constructor(
    private val networkClient: NetworkClient,
) {

    /**
     * Resolve a URL to real media metadata + real downloadable formats.
     *
     * @param url The shared media URL
     * @param plugin The detected platform plugin
     * @return ResolutionResult with either real formats or a specific error
     */
    suspend fun resolve(url: String, plugin: PlatformPlugin): ResolutionResult {
        // Check if platform has a real resolver
        if (!plugin.isResolvable) {
            return ResolutionResult.Error(
                ResolutionError.UNSUPPORTED_PLATFORM,
                "منصة ${plugin.nameAr} غير مدعومة حالياً — لا يتوفر محلل وسائط لهذه المنصة"
            )
        }

        // Check internet connectivity
        if (!networkClient.isOnline()) {
            return ResolutionResult.Error(
                ResolutionError.NETWORK_FAILURE,
                "لا يوجد اتصال بالإنترنت — تحقق من اتصالك وحاول مرة أخرى"
            )
        }

        // Delegate to platform-specific resolver
        return when (plugin.id) {
            "youtube" -> resolveYouTube(url, plugin)
            "vimeo" -> resolveVimeo(url, plugin)
            "soundcloud" -> resolveSoundCloud(url, plugin)
            "dailymotion" -> resolveDailymotion(url, plugin)
            "streamable" -> resolveStreamable(url, plugin)
            else -> ResolutionResult.Error(
                ResolutionError.UNSUPPORTED_PLATFORM,
                "لا يتوفر محلل لمنصة ${plugin.nameAr}"
            )
        }
    }

    // ============ YouTube Resolver ============
    // Uses YouTube's public oEmbed + get_video_info approach

    private suspend fun resolveYouTube(url: String, plugin: PlatformPlugin): ResolutionResult {
        return try {
            // Step 1: Get metadata via oEmbed
            val oembedUrl = "https://www.youtube.com/oembed?url=${java.net.URLEncoder.encode(url, "UTF-8")}&format=json"
            val oembedJson = networkClient.fetchJson(oembedUrl)
                ?: return ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, "فشل في جلب بيانات يوتيوب")

            val json = JSONObject(oembedJson)
            val title = json.optString("title", "").ifEmpty { null }
                ?: return ResolutionResult.Error(ResolutionError.NO_MEDIA_FOUND, "لم يتم العثور على عنوان للفيديو")

            val author = json.optString("author_name", "").ifEmpty { null }
            val thumbnailUrl = json.optString("thumbnail_url", "").ifEmpty { null }

            // Step 2: Extract video ID
            val videoId = extractYouTubeVideoId(url)
                ?: return ResolutionResult.Error(ResolutionError.NO_MEDIA_FOUND, "تعذّر استخراج معرف الفيديو")

            // Step 3: Try to get real download URLs via get_video_info
            val formats = resolveYouTubeFormats(videoId, plugin.id)

            if (formats.isEmpty()) {
                // YouTube requires specialized extraction (yt-dlp/sponsorblock etc.)
                // We cannot extract real download URLs with simple HTTP — report honestly
                return ResolutionResult.Error(
                    ResolutionError.PLATFORM_CHANGED,
                    "يتطلب يوتيوب محرك استخراج متخصص (yt-dlp). التنزيل المباشر غير متاح حالياً."
                )
            }

            val metadata = MediaMetadata(
                title = title,
                author = author,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = null,
                description = "من يوتيوب",
                sourceUrl = url,
                platformId = plugin.id,
                formats = emptyList(),
                resolvedAt = System.currentTimeMillis(),
            )

            ResolutionResult.Success(metadata, formats)
        } catch (e: Exception) {
            ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, e.message ?: "خطأ في الاتصال بيوتيوب")
        }
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            Regex("(?:v=|youtu\\.be/|shorts/)([\\w-]{11})"),
            Regex("/embed/([\\w-]{11})"),
            Regex("/live/([\\w-]{11})"),
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private suspend fun resolveYouTubeFormats(videoId: String, platformId: String): List<MediaFormat> {
        // YouTube actively blocks direct download URL extraction.
        // Real extraction requires yt-dlp or similar engine.
        // Return empty — the resolver will report PLATFORM_CHANGED honestly.
        return emptyList()
    }

    // ============ Vimeo Resolver ============
    // Vimeo has a public oEmbed API that provides direct video URLs

    private suspend fun resolveVimeo(url: String, plugin: PlatformPlugin): ResolutionResult {
        return try {
            val oembedUrl = "https://vimeo.com/api/oembed.json?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
            val oembedJson = networkClient.fetchJson(oembedUrl)
                ?: return ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, "فشل في جلب بيانات Vimeo")

            val json = JSONObject(oembedJson)
            val title = json.optString("title", "").ifEmpty { null }
                ?: return ResolutionResult.Error(ResolutionError.NO_MEDIA_FOUND, "لم يتم العثور على الفيديو")

            val author = json.optString("author_name", "").ifEmpty { null }
            val thumbnailUrl = json.optString("thumbnail_url", "").ifEmpty { null }
            val duration = json.optInt("duration", 0).takeIf { it > 0 }

            // Vimeo oEmbed doesn't provide direct download URLs
            // Need to check if the video is downloadable via Vimeo API
            val formats = mutableListOf<MediaFormat>()

            // Try to extract direct video URL from page HTML
            val html = networkClient.fetchText(url)
            if (html != null) {
                // Look for progressive download URLs in Vimeo player config
                val configPattern = Pattern.compile("\"url\":\"(https://[^\"]+\\.mp4[^\"]*)\"", Pattern.CASE_INSENSITIVE)
                val matcher = configPattern.matcher(html)
                while (matcher.find()) {
                    val directUrl = matcher.group(1)?.replace("\\/", "/")
                    if (directUrl != null && isValidMediaUrl(directUrl)) {
                        val quality = extractQualityFromUrl(directUrl)
                        formats.add(
                            MediaFormat(
                                id = "${plugin.id}-video-${quality.label}",
                                quality = quality,
                                ext = "mp4",
                                sizeBytes = 0L,
                                mediaType = MediaType.VIDEO,
                                hasAudio = true,
                                label = "Vimeo — ${quality.label.uppercase()}",
                                downloadUrl = directUrl,
                            ),
                        )
                    }
                }
            }

            if (formats.isEmpty()) {
                return ResolutionResult.Error(
                    ResolutionError.PLATFORM_CHANGED,
                    "يتطلب Vimeo محرك استخراج متخصص. التنزيل المباشر غير متاح."
                )
            }

            val metadata = MediaMetadata(
                title = title,
                author = author,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = duration,
                description = "من Vimeo",
                sourceUrl = url,
                platformId = plugin.id,
                formats = emptyList(),
                resolvedAt = System.currentTimeMillis(),
            )

            ResolutionResult.Success(metadata, formats)
        } catch (e: Exception) {
            ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, e.message ?: "خطأ في الاتصال بـ Vimeo")
        }
    }

    // ============ SoundCloud Resolver ============
    // SoundCloud oEmbed + API can provide direct stream URLs

    private suspend fun resolveSoundCloud(url: String, plugin: PlatformPlugin): ResolutionResult {
        return try {
            val oembedUrl = "https://soundcloud.com/oembed?format=json&url=${java.net.URLEncoder.encode(url, "UTF-8")}"
            val oembedJson = networkClient.fetchJson(oembedUrl)
                ?: return ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, "فشل في جلب بيانات SoundCloud")

            val json = JSONObject(oembedJson)
            val title = json.optString("title", "").ifEmpty { null }
                ?: return ResolutionResult.Error(ResolutionError.NO_MEDIA_FOUND, "لم يتم العثور على المحتوى")

            val author = json.optString("author_name", "").ifEmpty { null }
            val thumbnailUrl = json.optString("thumbnail_url", "").ifEmpty { null }

            // SoundCloud requires API key for direct stream URLs
            // Report honestly that we need backend service
            ResolutionResult.Error(
                ResolutionError.PLATFORM_CHANGED,
                "يتطلب SoundCloud مفتاح API لاستخراج روابط التنزيل. غير متاح حالياً."
            )
        } catch (e: Exception) {
            ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, e.message ?: "خطأ في الاتصال بـ SoundCloud")
        }
    }

    // ============ Dailymotion Resolver ============
    // Dailymotion has a public API that provides video URLs

    private suspend fun resolveDailymotion(url: String, plugin: PlatformPlugin): ResolutionResult {
        return try {
            // Extract video ID
            val videoId = Regex("dailymotion\\.com/video/([\\w]+)|dai\\.ly/([\\w]+)")
                .find(url)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
                ?: return ResolutionResult.Error(ResolutionError.NO_MEDIA_FOUND, "تعذّر استخراج معرف الفيديو")

            // Use Dailymotion API
            val apiUrl = "https://api.dailymotion.com/video/$videoId?fields=title,owner.screenname,thumbnail_url,duration,stream_url,stream_h264_url,stream_hls_url"
            val apiJson = networkClient.fetchJson(apiUrl)
                ?: return ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, "فشل في جلب بيانات Dailymotion")

            val json = JSONObject(apiJson)
            val title = json.optString("title", "").ifEmpty { null }
                ?: return ResolutionResult.Error(ResolutionError.NO_MEDIA_FOUND, "لم يتم العثور على الفيديو")

            val author = json.optString("owner.screenname", "").ifEmpty { null }
            val thumbnailUrl = json.optString("thumbnail_url", "").ifEmpty { null }
            val duration = json.optInt("duration", 0).takeIf { it > 0 }

            val formats = mutableListOf<MediaFormat>()

            // Try direct stream URLs
            val streamUrl = json.optString("stream_url", "").ifEmpty { null }
            val h264Url = json.optString("stream_h264_url", "").ifEmpty { null }

            if (streamUrl != null && isValidMediaUrl(streamUrl)) {
                formats.add(
                    MediaFormat(
                        id = "${plugin.id}-video-stream",
                        quality = MediaQuality.P720,
                        ext = "mp4",
                        sizeBytes = 0L,
                        mediaType = MediaType.VIDEO,
                        hasAudio = true,
                        label = "Dailymotion — مباشر",
                        downloadUrl = streamUrl,
                    ),
                )
            }

            if (h264Url != null && isValidMediaUrl(h264Url)) {
                formats.add(
                    MediaFormat(
                        id = "${plugin.id}-video-h264",
                        quality = MediaQuality.P480,
                        ext = "mp4",
                        sizeBytes = 0L,
                        mediaType = MediaType.VIDEO,
                        hasAudio = true,
                        label = "Dailymotion — H.264",
                        downloadUrl = h264Url,
                    ),
                )
            }

            if (formats.isEmpty()) {
                return ResolutionResult.Error(
                    ResolutionError.PLATFORM_CHANGED,
                    "لم يتم العثور على روابط تنزيل مباشرة من Dailymotion"
                )
            }

            val metadata = MediaMetadata(
                title = title,
                author = author,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = duration,
                description = "من Dailymotion",
                sourceUrl = url,
                platformId = plugin.id,
                formats = emptyList(),
                resolvedAt = System.currentTimeMillis(),
            )

            ResolutionResult.Success(metadata, formats)
        } catch (e: Exception) {
            ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, e.message ?: "خطأ في الاتصال بـ Dailymotion")
        }
    }

    // ============ Streamable Resolver ============
    // Streamable has a simple API that returns direct video URLs

    private suspend fun resolveStreamable(url: String, plugin: PlatformPlugin): ResolutionResult {
        return try {
            val shortcode = Regex("streamable\\.com/([\\w]+)").find(url)?.groupValues?.get(1)
                ?: return ResolutionResult.Error(ResolutionError.NO_MEDIA_FOUND, "تعذّر استخراج معرف الفيديو")

            val apiUrl = "https://api.streamable.com/videos/$shortcode"
            val apiJson = networkClient.fetchJson(apiUrl)
                ?: return ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, "فشل في جلب بيانات Streamable")

            val json = JSONObject(apiJson)
            val videoObj = json.optJSONObject("video") ?: return ResolutionResult.Error(ResolutionError.NO_MEDIA_FOUND, "لم يتم العثور على الفيديو")

            val title = videoObj.optString("title", "").ifEmpty { "Streamable — $shortcode" }
            val thumbnailUrl = videoObj.optString("thumbnail_url", "").ifEmpty { null }

            val formats = mutableListOf<MediaFormat>()

            // Streamable provides mp4 and mp4-mobile URLs
            val mp4Url = videoObj.optString("url", "").ifEmpty { null }
            val mp4MobileUrl = videoObj.optString("mobile", "").ifEmpty { null }

            if (mp4Url != null && isValidMediaUrl(mp4Url)) {
                formats.add(
                    MediaFormat(
                        id = "${plugin.id}-video-hd",
                        quality = MediaQuality.P720,
                        ext = "mp4",
                        sizeBytes = 0L,
                        mediaType = MediaType.VIDEO,
                        hasAudio = true,
                        label = "Streamable — عالية الجودة",
                        downloadUrl = mp4Url,
                    ),
                )
            }

            if (mp4MobileUrl != null && isValidMediaUrl(mp4MobileUrl)) {
                formats.add(
                    MediaFormat(
                        id = "${plugin.id}-video-mobile",
                        quality = MediaQuality.P360,
                        ext = "mp4",
                        sizeBytes = 0L,
                        mediaType = MediaType.VIDEO,
                        hasAudio = true,
                        label = "Streamable — جودة منخفضة",
                        downloadUrl = mp4MobileUrl,
                    ),
                )
            }

            if (formats.isEmpty()) {
                return ResolutionResult.Error(
                    ResolutionError.PLATFORM_CHANGED,
                    "لم يتم العثور على روابط تنزيل من Streamable"
                )
            }

            val metadata = MediaMetadata(
                title = title,
                author = "Streamable",
                thumbnailUrl = thumbnailUrl,
                durationSeconds = null,
                description = "من Streamable",
                sourceUrl = url,
                platformId = plugin.id,
                formats = emptyList(),
                resolvedAt = System.currentTimeMillis(),
            )

            ResolutionResult.Success(metadata, formats)
        } catch (e: Exception) {
            ResolutionResult.Error(ResolutionError.NETWORK_FAILURE, e.message ?: "خطأ في الاتصال بـ Streamable")
        }
    }

    // ============ Utilities ============

    private fun isValidMediaUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false

        val mediaExtensions = listOf(".mp4", ".webm", ".m4a", ".mp3", ".ogg", ".wav", ".avi", ".mov", ".mkv", ".flv", ".m3u8", ".mpd")
        if (mediaExtensions.any { lower.contains(it) }) return true

        if (lower.contains("googlevideo.com") || lower.contains("fbcdn.net") || lower.contains("akamaized.net") || lower.contains("cloudfront.net")) {
            return true
        }

        return false
    }

    private fun extractQualityFromUrl(url: String): MediaQuality {
        val lower = url.lowercase()
        return when {
            lower.contains("1080") -> MediaQuality.P1080
            lower.contains("720") -> MediaQuality.P720
            lower.contains("480") -> MediaQuality.P480
            lower.contains("360") -> MediaQuality.P360
            lower.contains("240") -> MediaQuality.P240
            else -> MediaQuality.P720
        }
    }
}
