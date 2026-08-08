package com.videohub.pro.network

import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata
import com.videohub.pro.domain.models.MediaQuality
import com.videohub.pro.domain.models.MediaType
import com.videohub.pro.plugins.PlatformPlugin
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real metadata extractor — fetches actual video information from the internet.
 *
 * CRITICAL RULE: A webpage URL must NEVER be used as a downloadUrl.
 * Only verified media resource URLs (from og:video, <source>, direct file links)
 * are used as downloadUrl. If no media URL is found, formats will be empty
 * and the UI will show "Unable to resolve media from this URL".
 */
@Singleton
class MetadataExtractor @Inject constructor(
    private val networkClient: NetworkClient,
) {

    /**
     * Extract real metadata + real download URLs from a URL.
     * Returns metadata with formats that have REAL downloadUrls, or empty formats if none found.
     */
    suspend fun extract(url: String, plugin: PlatformPlugin): MediaMetadata {
        // Step 1: Try oEmbed API for real metadata (title, author, thumbnail)
        val oembedResult = tryOEmbed(url, plugin.id, plugin.nameAr)
        if (oembedResult != null) {
            // oEmbed gives us metadata but NOT a download URL.
            // We need to also try HTML scraping for the actual media URL.
            val htmlResult = tryHtmlMetaTags(url, plugin)
            if (htmlResult != null && htmlResult.formats.isNotEmpty()) {
                // Merge: use oEmbed metadata + HTML-extracted download URLs
                return oembedResult.copy(
                    formats = htmlResult.formats,
                    thumbnailUrl = oembedResult.thumbnailUrl ?: htmlResult.thumbnailUrl,
                )
            }
            // oEmbed metadata found but no download URL — return with empty formats
            return oembedResult.copy(formats = emptyList())
        }

        // Step 2: Try HTML meta tags for both metadata AND download URLs
        val htmlResult = tryHtmlMetaTags(url, plugin)
        if (htmlResult != null) {
            return htmlResult
        }

        // Step 3: Cannot resolve — return metadata with empty formats (no downloadUrl)
        return MediaMetadata(
            title = "${plugin.nameAr} — ${plugin.identify(url)}",
            author = plugin.name,
            thumbnailUrl = null,
            durationSeconds = null,
            description = "تعذّر استخراج معلومات الوسائط من هذا الرابط",
            sourceUrl = url,
            platformId = plugin.id,
            formats = emptyList(), // EMPTY — no fake download URLs
            resolvedAt = System.currentTimeMillis(),
        )
    }

    private fun tryOEmbed(url: String, platformId: String, platformNameAr: String): MediaMetadata? {
        val oembedEndpoint = when (platformId) {
            "youtube" -> "https://www.youtube.com/oembed?url=${java.net.URLEncoder.encode(url, "UTF-8")}&format=json"
            "vimeo" -> "https://vimeo.com/api/oembed.json?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
            "soundcloud" -> "https://soundcloud.com/oembed?format=json&url=${java.net.URLEncoder.encode(url, "UTF-8")}"
            else -> return null
        }

        val jsonStr = networkClient.fetchJson(oembedEndpoint) ?: return null

        return try {
            val json = JSONObject(jsonStr)
            val title = json.optString("title", "").ifEmpty { null } ?: return null
            val author = json.optString("author_name", "").ifEmpty { null }
            val thumbnailUrl = json.optString("thumbnail_url", "").ifEmpty { null }
            val durationSeconds = json.optInt("duration", 0).takeIf { it > 0 }

            MediaMetadata(
                title = title,
                author = author,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                description = "من $platformNameAr",
                sourceUrl = url,
                platformId = platformId,
                formats = emptyList(), // oEmbed doesn't provide download URLs
                resolvedAt = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun tryHtmlMetaTags(url: String, plugin: PlatformPlugin): MediaMetadata? {
        val html = networkClient.fetchText(url) ?: return null

        val title = extractMetaTag(html, "og:title")
            ?: extractMetaTag(html, "twitter:title")
            ?: extractTitleTag(html)
            ?: return null

        val author = extractMetaTag(html, "og:site_name")
            ?: extractMetaTag(html, "article:author")
            ?: plugin.nameAr

        val thumbnailUrl = extractMetaTag(html, "og:image")
            ?: extractMetaTag(html, "twitter:image")

        val description = extractMetaTag(html, "og:description")

        val durationStr = extractMetaTag(html, "og:video:duration")
        val durationSeconds = durationStr?.toIntOrNull()

        // Extract REAL download URLs from meta tags — ONLY verified media URLs
        val videoDownloadUrl = extractMetaTag(html, "og:video")
            ?: extractMetaTag(html, "og:video:url")
            ?: extractMetaTag(html, "og:video:secure_url")
            ?: extractVideoSrcFromHtml(html)

        val audioDownloadUrl = extractMetaTag(html, "og:audio")
            ?: extractMetaTag(html, "og:audio:url")

        // Build formats — ONLY with verified media URLs
        // CRITICAL: Do NOT use the webpage URL as a downloadUrl
        val formats = mutableListOf<MediaFormat>()

        if (videoDownloadUrl != null && isValidMediaUrl(videoDownloadUrl)) {
            formats.add(
                MediaFormat(
                    id = "${plugin.id}-video-direct",
                    quality = MediaQuality.P720,
                    ext = guessExtFromUrl(videoDownloadUrl, "mp4"),
                    sizeBytes = 0L,
                    mediaType = MediaType.VIDEO,
                    hasAudio = true,
                    label = "فيديو — أفضل جودة متاحة",
                    downloadUrl = videoDownloadUrl,
                ),
            )
        }

        if (audioDownloadUrl != null && isValidMediaUrl(audioDownloadUrl)) {
            formats.add(
                MediaFormat(
                    id = "${plugin.id}-audio-direct",
                    quality = MediaQuality.AUDIO,
                    ext = guessExtFromUrl(audioDownloadUrl, "mp3"),
                    sizeBytes = 0L,
                    mediaType = MediaType.AUDIO,
                    hasAudio = true,
                    bitrate = 128,
                    label = "صوت — استخراج مباشر",
                    downloadUrl = audioDownloadUrl,
                ),
            )
        }

        // If no media URLs found — formats stays EMPTY (not a fake fallback)
        // The UI will show "Unable to resolve media from this URL"

        return MediaMetadata(
            title = title,
            author = author,
            thumbnailUrl = thumbnailUrl,
            durationSeconds = durationSeconds,
            description = description ?: "من ${plugin.nameAr}",
            sourceUrl = url,
            platformId = plugin.id,
            formats = formats, // May be empty — that's correct
            resolvedAt = System.currentTimeMillis(),
        )
    }

    /**
     * Validate that a URL points to an actual media resource, not a webpage.
     * Rejects URLs that look like HTML pages.
     */
    private fun isValidMediaUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()

        // Must be http or https
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false

        // Reject obvious webpage URLs (no file extension and no media indicators)
        val mediaExtensions = listOf(".mp4", ".webm", ".m4a", ".mp3", ".ogg", ".wav", ".avi", ".mov", ".mkv", ".flv", ".m3u8", ".mpd")
        val hasMediaExt = mediaExtensions.any { lower.contains(it) }

        // Allow URLs with media extensions
        if (hasMediaExt) return true

        // Allow URLs that contain "video" or "media" or "stream" or "download" in path
        if (lower.contains("/video/") || lower.contains("/media/") || lower.contains("/stream/") || lower.contains("/download/")) {
            return true
        }

        // Allow googlevideo.com, googleusercontent.com, fbcdn.net, etc.
        if (lower.contains("googlevideo.com") || lower.contains("fbcdn.net") || lower.contains("akamaized.net") || lower.contains("cloudfront.net")) {
            return true
        }

        // Reject if it looks like the same webpage URL (ends with / or has query params typical of web pages)
        return false
    }

    // ============ HTML Parsing Utilities ============

    private fun extractMetaTag(html: String, tag: String): String? {
        val patterns = listOf(
            Pattern.compile("<meta\\s+property=[\"']$tag[\"']\\s+content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<meta\\s+name=[\"']$tag[\"']\\s+content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<meta\\s+content=[\"']([^\"']*)[\"']\\s+property=[\"']$tag[\"']", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<meta\\s+content=[\"']([^\"']*)[\"']\\s+name=[\"']$tag[\"']", Pattern.CASE_INSENSITIVE),
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val value = matcher.group(1)
                if (!value.isNullOrEmpty()) {
                    return decodeHtmlEntities(value)
                }
            }
        }
        return null
    }

    private fun extractTitleTag(html: String): String? {
        val pattern = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val title = matcher.group(1)?.trim()
            if (!title.isNullOrEmpty()) {
                return decodeHtmlEntities(title)
            }
        }
        return null
    }

    private fun extractVideoSrcFromHtml(html: String): String? {
        val videoSrcPattern = Pattern.compile("<video[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher1 = videoSrcPattern.matcher(html)
        if (matcher1.find()) return matcher1.group(1)

        val sourcePattern = Pattern.compile("<source[^>]+src=[\"']([^\"']+)[\"'][^>]+type=[\"']video/", Pattern.CASE_INSENSITIVE)
        val matcher2 = sourcePattern.matcher(html)
        if (matcher2.find()) return matcher2.group(1)

        val sourcePattern2 = Pattern.compile("<source[^>]+type=[\"']video/[^\"']*[\"'][^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        val matcher3 = sourcePattern2.matcher(html)
        if (matcher3.find()) return matcher3.group(1)

        return null
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
    }

    private fun guessExtFromUrl(url: String, default: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".mp4") -> "mp4"
            lower.contains(".webm") -> "webm"
            lower.contains(".m4a") -> "m4a"
            lower.contains(".mp3") -> "mp3"
            lower.contains(".ogg") -> "ogg"
            lower.contains(".wav") -> "wav"
            lower.contains(".avi") -> "avi"
            lower.contains(".mov") -> "mov"
            else -> default
        }
    }
}
