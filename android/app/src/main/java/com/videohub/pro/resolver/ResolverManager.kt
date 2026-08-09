package com.videohub.pro.resolver

import android.content.Context
import android.util.Log
import com.videohub.pro.network.NetworkClient
import com.videohub.pro.plugins.PluginRegistry
import com.videohub.pro.resolver.ytdlp.YtDlpResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolver Manager — central orchestrator for media resolution.
 *
 * Architecture (embedded yt-dlp via Chaquopy 17):
 *   Kotlin → YtDlpResolver → Chaquopy → Python 3.13 → yt-dlp → HTTP → platform CDNs
 *
 * All platforms are resolved by the embedded yt-dlp. No external server needed.
 *
 * Authentication: For platforms requiring login, the AuthenticationManager provides
 * cookies which are passed through to yt-dlp. We do NOT bypass DRM, CAPTCHA, or MFA.
 */
@Singleton
class ResolverManager @Inject constructor(
    private val pluginRegistry: PluginRegistry,
    private val networkClient: NetworkClient,
    private val ytDlpResolver: YtDlpResolver,
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "ResolverManager"
    }

    /**
     * Initialize Python + yt-dlp. Call this on app startup.
     */
    fun initialize() {
        ytDlpResolver.initialize()
    }

    /**
     * Check if the embedded resolver is ready.
     */
    suspend fun isReady(): Boolean {
        return ytDlpResolver.isReady()
    }

    /**
     * Get the yt-dlp version string.
     */
    fun getVersion(): String = ytDlpResolver.getVersion()

    /**
     * Resolve a URL to real media formats using embedded yt-dlp.
     *
     * Supports 1000+ platforms: YouTube, TikTok, Facebook, Instagram, Twitter/X,
     * Vimeo, Dailymotion, Reddit, Twitch, SoundCloud, Pinterest, LinkedIn,
     * Tumblr, Streamable, and many more.
     *
     * @param url The media URL to resolve
     * @param taskId Unique task ID for diagnostics
     * @param authContext Optional authentication context (cookies from AuthenticationManager)
     */
    suspend fun resolve(
        url: String,
        taskId: String,
        authContext: AuthenticationContext? = null,
    ): ResolveResult {
        Log.i(TAG, "Resolving URL: $url (taskId: $taskId) via embedded yt-dlp")

        // Step 1: Detect platform (for UI display only — yt-dlp handles all platforms)
        val plugin = pluginRegistry.findForUrl(url)
        val platformId = plugin?.id ?: "generic"
        val platformName = plugin?.nameAr ?: "عام"

        Log.i(TAG, "Detected platform: $platformId ($platformName)")

        // Step 2: Check internet connectivity
        if (!networkClient.isOnline()) {
            return ResolveResult(
                taskId = taskId, platformId = platformId, contentId = null,
                status = ResolveStatus.FAILED,
                error = ResolveError.NETWORK_FAILURE,
                errorMessage = "لا يوجد اتصال بالإنترنت",
            )
        }

        // Step 3: Resolve via embedded yt-dlp
        val result = ytDlpResolver.resolve(url, taskId, authContext)

        Log.i(
            TAG,
            "Resolution result: status=${result.status}, formats=${result.formats.size}, error=${result.error}"
        )
        return result
    }

    /**
     * Get platform capabilities — all platforms are now resolvable via yt-dlp.
     */
    fun getCapabilities(): List<PlatformCapability> {
        return pluginRegistry.all().map { plugin ->
            PlatformCapability(
                platformId = plugin.id,
                detectable = true,
                resolvable = true, // yt-dlp supports all
                downloadable = true,
                authRequired = plugin.id in listOf("instagram", "linkedin"),
                authSupported = plugin.id in listOf("facebook", "instagram", "youtube", "x"),
                backendRequired = false, // No external backend needed
                temporarilyUnavailable = false,
            )
        }
    }
}
