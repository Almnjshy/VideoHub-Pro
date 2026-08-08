package com.videohub.pro.plugins

/**
 * Plugin Registry — registers all platform plugins.
 * Only platforms with isResolvable=true have real media extraction.
 */
class PluginRegistry {
    private val plugins: List<PlatformPlugin> = listOf(
        YouTubePlugin(),
        FacebookPlugin(),
        TikTokPlugin(),
        XPlugin(),
        InstagramPlugin(),
        VimeoPlugin(),
        DailymotionPlugin(),
        RedditPlugin(),
        TwitchPlugin(),
        SoundCloudPlugin(),
        PinterestPlugin(),
        LinkedInPlugin(),
        TumblrPlugin(),
        StreamablePlugin(),
    )

    private val pluginMap: Map<String, PlatformPlugin> = plugins.associateBy { it.id }

    fun all(): List<PlatformPlugin> = plugins

    fun get(id: String): PlatformPlugin? = pluginMap[id]

    fun findForUrl(url: String): PlatformPlugin? = plugins.find { it.canHandle(url) }

    fun count(): Int = plugins.size

    /** Returns only platforms that have real resolvers */
    fun resolvable(): List<PlatformPlugin> = plugins.filter { it.isResolvable }
}
