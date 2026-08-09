package com.videohub.pro.plugins

import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata

/**
 * Platform Plugin Interface — handles URL detection only.
 * Media resolution is handled by PlatformResolver implementations.
 *
 * A platform is considered "supported" only when its resolver can
 * actually return valid media formats with real download URLs.
 */
interface PlatformPlugin {
    val id: String
    val name: String
    val nameAr: String
    val icon: String
    val color: String
    val version: String

    /** Returns true if this plugin can identify the URL's platform */
    fun canHandle(url: String): Boolean

    /** Extract a content identifier from the URL */
    fun identify(url: String): String

    /**
     * Returns true if this platform has a REAL resolver that can extract
     * actual media download URLs. Platforms without real resolvers
     * will show "unsupported" to the user.
     */
    val isResolvable: Boolean
        get() = false
}

/**
 * Base implementation of [PlatformPlugin] that supplies a configurable
 * [isResolvable] flag. Concrete plugins extend this class and only need
 * to override the metadata + detection members.
 */
abstract class BasePlatformPlugin(
    override val isResolvable: Boolean = false,
) : PlatformPlugin
