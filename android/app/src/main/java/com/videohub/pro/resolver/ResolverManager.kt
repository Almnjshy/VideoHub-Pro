package com.videohub.pro.resolver

import android.content.Context
import android.util.Log
import com.videohub.pro.network.NetworkClient
import com.videohub.pro.plugins.PlatformPlugin
import com.videohub.pro.plugins.PluginRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolver Manager — central orchestrator for media resolution.
 *
 * Responsibilities:
 * 1. Detect platform from URL
 * 2. Determine platform capabilities
 * 3. Select the correct platform-specific resolver
 * 4. Pass the current taskId — prevent cross-task contamination
 * 5. Return structured errors
 *
 * Does NOT contain platform-specific extraction logic — delegates to per-platform resolvers.
 */
@Singleton
class ResolverManager @Inject constructor(
    private val pluginRegistry: PluginRegistry,
    private val networkClient: NetworkClient,
    private val youTubeResolver: YouTubeResolver,
    private val facebookResolver: FacebookResolver,
    private val tiktokResolver: TikTokResolver,
    private val instagramResolver: InstagramResolver,
    private val twitterResolver: TwitterResolver,
    private val redditResolver: RedditResolver,
    private val twitchResolver: TwitchResolver,
    private val soundCloudResolver: SoundCloudResolver,
    private val pinterestResolver: PinterestResolver,
    private val linkedInResolver: LinkedInResolver,
    private val tumblrResolver: TumblrResolver,
    // Existing working resolvers (kept from previous implementation)
    private val mediaResolver: MediaResolver,
) {

    companion object {
        private const val TAG = "ResolverManager"
    }

    /**
     * Resolve a URL to real media formats.
     * Each call is tied to a unique taskId — no cross-task contamination.
     */
    suspend fun resolve(url: String, taskId: String, authContext: AuthenticationContext? = null): ResolveResult {
        Log.i(TAG, "Resolving URL: $url (taskId: $taskId)")

        // Step 1: Detect platform
        val plugin = pluginRegistry.findForUrl(url)
            ?: return ResolveResult(
                taskId = taskId, platformId = "unknown", contentId = null,
                status = ResolveStatus.UNSUPPORTED,
                error = ResolveError.UNSUPPORTED_PLATFORM,
                errorMessage = "لا توجد منصة مدعومة لهذا الرابط",
            )

        Log.i(TAG, "Detected platform: ${plugin.id} (${plugin.nameAr})")

        // Step 2: Check if platform is resolvable
        if (!plugin.isResolvable) {
            return ResolveResult(
                taskId = taskId, platformId = plugin.id, contentId = plugin.identify(url),
                status = ResolveStatus.UNSUPPORTED,
                error = ResolveError.UNSUPPORTED_PLATFORM,
                errorMessage = "منصة ${plugin.nameAr} غير مدعومة حالياً",
            )
        }

        // Step 3: Check internet connectivity
        if (!networkClient.isOnline()) {
            return ResolveResult(
                taskId = taskId, platformId = plugin.id, contentId = plugin.identify(url),
                status = ResolveStatus.FAILED,
                error = ResolveError.NETWORK_FAILURE,
                errorMessage = "لا يوجد اتصال بالإنترنت",
            )
        }

        // Step 4: Build resolve request with unique taskId
        val request = ResolveRequest(
            taskId = taskId,
            sourceUrl = url,
            platformId = plugin.id,
            contentId = plugin.identify(url),
            authenticationContext = authContext,
        )

        // Step 5: Delegate to platform-specific resolver
        val result = when (plugin.id) {
            "youtube" -> youTubeResolver.resolve(request)
            "facebook" -> facebookResolver.resolve(request)
            "tiktok" -> tiktokResolver.resolve(request)
            "instagram" -> instagramResolver.resolve(request)
            "x" -> twitterResolver.resolve(request)
            "reddit" -> redditResolver.resolve(request)
            "twitch" -> twitchResolver.resolve(request)
            "soundcloud" -> soundCloudResolver.resolve(request)
            "pinterest" -> pinterestResolver.resolve(request)
            "linkedin" -> linkedInResolver.resolve(request)
            "tumblr" -> tumblrResolver.resolve(request)
            // Existing working resolvers
            "vimeo", "dailymotion", "streamable" -> {
                // Delegate to existing MediaResolver which handles these
                val oldResult = mediaResolver.resolve(url, plugin)
                // Convert old ResolutionResult to new ResolveResult
                convertOldResult(oldResult, taskId, plugin.id, request.contentId)
            }
            else -> ResolveResult(
                taskId = taskId, platformId = plugin.id, contentId = request.contentId,
                status = ResolveStatus.UNSUPPORTED,
                error = ResolveError.UNSUPPORTED_PLATFORM,
                errorMessage = "لا يتوفر محلل لمنصة ${plugin.nameAr}",
            )
        }

        Log.i(TAG, "Resolution result: status=${result.status}, formats=${result.formats.size}, error=${result.error}")
        return result
    }

    /**
     * Get platform capabilities — reports what each resolver can actually do.
     */
    fun getCapabilities(): List<PlatformCapability> {
        return pluginRegistry.all().map { plugin ->
            PlatformCapability(
                platformId = plugin.id,
                detectable = true,
                resolvable = plugin.isResolvable,
                downloadable = plugin.isResolvable && plugin.id in listOf("vimeo", "dailymotion", "streamable", "reddit", "pinterest", "tumblr"),
                authRequired = plugin.id in listOf("instagram", "linkedin"),
                authSupported = plugin.id in listOf("facebook", "instagram", "youtube", "x"),
                backendRequired = plugin.id in listOf("twitch", "soundcloud"),
                temporarilyUnavailable = false,
            )
        }
    }

    /**
     * Convert old ResolutionResult to new ResolveResult format.
     */
    private fun convertOldResult(
        oldResult: ResolutionResult,
        taskId: String,
        platformId: String,
        contentId: String?,
    ): ResolveResult {
        return when (oldResult) {
            is ResolutionResult.Success -> ResolveResult(
                taskId = taskId,
                platformId = platformId,
                contentId = contentId,
                status = ResolveStatus.RESOLVED,
                metadata = oldResult.metadata,
                formats = oldResult.formats,
                resolverEngine = "LocalExtractionEngine",
            )
            is ResolutionResult.Error -> ResolveResult(
                taskId = taskId,
                platformId = platformId,
                contentId = contentId,
                status = when (oldResult.reason) {
                    ResolutionError.UNSUPPORTED_PLATFORM -> ResolveStatus.UNSUPPORTED
                    ResolutionError.AUTH_REQUIRED -> ResolveStatus.AUTHENTICATION_REQUIRED
                    ResolutionError.BACKEND_UNAVAILABLE -> ResolveStatus.BACKEND_REQUIRED
                    else -> ResolveStatus.FAILED
                },
                error = when (oldResult.reason) {
                    ResolutionError.UNSUPPORTED_PLATFORM -> ResolveError.UNSUPPORTED_PLATFORM
                    ResolutionError.AUTH_REQUIRED -> ResolveError.AUTHENTICATION_REQUIRED
                    ResolutionError.PRIVATE_CONTENT -> ResolveError.PRIVATE_CONTENT
                    ResolutionError.NO_MEDIA_FOUND -> ResolveError.NO_MEDIA_STREAM
                    ResolutionError.NETWORK_FAILURE -> ResolveError.NETWORK_FAILURE
                    ResolutionError.PLATFORM_CHANGED -> ResolveError.PLATFORM_CHANGED
                    ResolutionError.BACKEND_UNAVAILABLE -> ResolveError.BACKEND_UNAVAILABLE
                },
                errorMessage = oldResult.message,
            )
        }
    }
}
