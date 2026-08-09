package com.videohub.pro.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clipboard Monitor — يراقب الحافظة ويكتشف الروابط المنسوخة تلقائياً.
 *
 * عند نسخ رابط من أي تطبيق (YouTube, TikTok, Facebook, إلخ)،
 * يكتشفه هذا المراقب ويُظهر نافذة التنزيل تلقائياً.
 */
@Singleton
class ClipboardMonitor @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "ClipboardMonitor"
        private const val POLL_INTERVAL_MS = 1500L

        private val URL_PATTERNS = listOf(
            Regex("https?://(?:www\\.)?youtube\\.com/watch\\?v=[\\w-]+", RegexOption.IGNORE_CASE),
            Regex("https?://youtu\\.be/[\\w-]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.|m\\.)?tiktok\\.com/[\\w/@]+", RegexOption.IGNORE_CASE),
            Regex("https?://vm\\.tiktok\\.com/[\\w]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?facebook\\.com/[\\w/.]+", RegexOption.IGNORE_CASE),
            Regex("https?://fb\\.watch/[\\w]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?instagram\\.com/(?:p|reel|tv)/[\\w-]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?(?:twitter|x)\\.com/[\\w]+/status/[\\d]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?vimeo\\.com/[\\d]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?dailymotion\\.com/video/[\\w]+", RegexOption.IGNORE_CASE),
            Regex("https?://dai\\.ly/[\\w]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?reddit\\.com/r/[\\w]+/comments/[\\w]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?twitch\\.tv/[\\w]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?soundcloud\\.com/[\\w/-]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?pinterest\\.com/pin/[\\w-]+", RegexOption.IGNORE_CASE),
            Regex("https?://pin\\.it/[\\w]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?streamable\\.com/[\\w]+", RegexOption.IGNORE_CASE),
            Regex("https?://(?:www\\.)?tumblr\\.com/[\\w/-]+", RegexOption.IGNORE_CASE),
        )
    }

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val _detectedUrl = MutableStateFlow<String?>(null)
    val detectedUrl: StateFlow<String?> = _detectedUrl.asStateFlow()

    private var lastClipText: String = ""
    private var isMonitoring = false
    private var pollThread: Thread? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard()
    }

    fun start() {
        if (isMonitoring) return
        isMonitoring = true
        try {
            clipboardManager.addPrimaryClipChangedListener(clipListener)
        } catch (e: Exception) {
            Log.w(TAG, "Clipboard listener failed, using polling", e)
        }
        pollThread = Thread {
            while (isMonitoring) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS)
                    checkClipboard()
                } catch (e: InterruptedException) { break }
                catch (e: Exception) { Log.w(TAG, "Polling error", e) }
            }
        }.also { it.isDaemon = true; it.start() }
        Log.i(TAG, "Clipboard monitoring started")
    }

    fun stop() {
        isMonitoring = false
        try { clipboardManager.removePrimaryClipChangedListener(clipListener) } catch (e: Exception) {}
        pollThread?.interrupt()
        pollThread = null
        Log.i(TAG, "Clipboard monitoring stopped")
    }

    private fun checkClipboard() {
        try {
            if (!clipboardManager.hasPrimaryClip()) return
            val clip = clipboardManager.primaryClip ?: return
            if (clip.itemCount == 0) return
            val text = clip.getItemAt(0).coerceToText(context).toString().trim()
            if (text == lastClipText || text.isEmpty()) return
            lastClipText = text
            val url = extractUrl(text)
            if (url != null) {
                Log.i(TAG, "Detected URL: $url")
                _detectedUrl.value = url
            }
        } catch (e: Exception) { Log.w(TAG, "Clipboard check failed", e) }
    }

    private fun extractUrl(text: String): String? {
        for (pattern in URL_PATTERNS) {
            pattern.find(text)?.let { return it.value }
        }
        val generic = Regex("https?://[\\w.-]+\\.[a-z]{2,}/[\\w/.-]*", RegexOption.IGNORE_CASE)
        return generic.find(text)?.value
    }

    fun consumeUrl() { _detectedUrl.value = null }
}
