package com.videohub.pro.plugins

// ============ ALL PLATFORMS: NOT_VERIFIED until tested on real device ============
class YouTubePlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "youtube"
    override val name = "YouTube"
    override val nameAr = "يوتيوب"
    override val icon = "▶"
    override val color = "#FF0000"
    override val version = "3.1.0"
    override fun canHandle(url: String) = Regex("youtube\\.com|youtu\\.be", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("(?:v=|youtu\\.be/|shorts/|live/|embed/)([\\w-]{11})").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ Facebook — RESOLVABLE (local extraction via HTML for public videos) ============
class FacebookPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "facebook"
    override val name = "Facebook"
    override val nameAr = "فيسبوك"
    override val icon = "f"
    override val color = "#1877F2"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("facebook\\.com|fb\\.watch|fb\\.com", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("(?:videos/|watch/\\?v=)(\\d+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ TikTok — RESOLVABLE (local extraction via HTML) ============
class TikTokPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "tiktok"
    override val name = "TikTok"
    override val nameAr = "تيك توك"
    override val icon = "♪"
    override val color = "#FE2C55"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("tiktok\\.com|vm\\.tiktok", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("/video/(\\d+)|vm\\.tiktok\\.com/([\\w]+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ X/Twitter — RESOLVABLE (local extraction via HTML) ============
class XPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "x"
    override val name = "X (Twitter)"
    override val nameAr = "إكس"
    override val icon = "𝕏"
    override val color = "#000000"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("twitter\\.com|x\\.com|t\\.co", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("/status/(\\d+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ Instagram — RESOLVABLE (requires auth, but resolver exists) ============
class InstagramPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "instagram"
    override val name = "Instagram"
    override val nameAr = "إنستغرام"
    override val icon = "◉"
    override val color = "#E4405F"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("instagram\\.com|instagr\\.am", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("/(?:p|reel|tv)/([\\w-]+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ Vimeo — RESOLVABLE (existing) ============
class VimeoPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "vimeo"
    override val name = "Vimeo"
    override val nameAr = "فيميو"
    override val icon = "V"
    override val color = "#1AB7EA"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("vimeo\\.com", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("vimeo\\.com/(\\d+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ Dailymotion — RESOLVABLE (existing) ============
class DailymotionPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "dailymotion"
    override val name = "Dailymotion"
    override val nameAr = "ديلي موشن"
    override val icon = "D"
    override val color = "#0066DC"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("dailymotion\\.com|dai\\.ly", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("dai\\.ly/([\\w]+)|video/([\\w]+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ Reddit — RESOLVABLE (public JSON API) ============
class RedditPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "reddit"
    override val name = "Reddit"
    override val nameAr = "ريديت"
    override val icon = "R"
    override val color = "#FF4500"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("reddit\\.com|r\\.reddit", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("/comments/([\\w]+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ Twitch — NOT resolvable locally (requires HLS backend) ============
class TwitchPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "twitch"
    override val name = "Twitch"
    override val nameAr = "تويتش"
    override val icon = "T"
    override val color = "#9146FF"
    override val version = "1.0.0"
    override fun canHandle(url: String) = Regex("twitch\\.tv|clips\\.twitch", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("/clip/([\\w-]+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ SoundCloud — NOT resolvable locally (requires API key) ============
class SoundCloudPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "soundcloud"
    override val name = "SoundCloud"
    override val nameAr = "ساوند كلاود"
    override val icon = "S"
    override val color = "#FF5500"
    override val version = "1.0.0"
    override fun canHandle(url: String) = Regex("soundcloud\\.com|snd\\.sc", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String = "sc"
}

// ============ Pinterest — RESOLVABLE (og:image/og:video extraction) ============
class PinterestPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "pinterest"
    override val name = "Pinterest"
    override val nameAr = "بينتريست"
    override val icon = "P"
    override val color = "#BD081C"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("pinterest\\.com|pin\\.it", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("/pin/([\\w-]+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}

// ============ LinkedIn — NOT resolvable (requires auth) ============
class LinkedInPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "linkedin"
    override val name = "LinkedIn"
    override val nameAr = "لينكدإن"
    override val icon = "in"
    override val color = "#0A66C2"
    override val version = "1.0.0"
    override fun canHandle(url: String) = Regex("linkedin\\.com|lnkd\\.in", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String = "li"
}

// ============ Tumblr — RESOLVABLE (og:video/og:image extraction) ============
class TumblrPlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "tumblr"
    override val name = "Tumblr"
    override val nameAr = "تمبلر"
    override val icon = "t"
    override val color = "#36465D"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("tumblr\\.com", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String = "tb"
}

// ============ Streamable — RESOLVABLE (existing) ============
class StreamablePlugin : BasePlatformPlugin(isResolvable = false) {
    override val id = "streamable"
    override val name = "Streamable"
    override val nameAr = "ستريمابل"
    override val icon = "St"
    override val color = "#0F90FA"
    override val version = "2.0.0"
    override fun canHandle(url: String) = Regex("streamable\\.com", RegexOption.IGNORE_CASE).containsMatchIn(url)
    override fun identify(url: String): String =
        Regex("streamable\\.com/([\\w]+)").find(url)?.groupValues?.getOrNull(1) ?: "unknown"
}
