package com.videohub.pro.resolver

import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata

// ============ Resolver Contract ============
// Shared between Android and Web — both must use the same request/response schema.

/**
 * Request to resolve a media URL to real downloadable formats.
 * Each request is tied to a specific taskId — no cross-task contamination.
 */
data class ResolveRequest(
    val taskId: String,
    val sourceUrl: String,
    val platformId: String,
    val contentId: String? = null,
    val authenticationContext: AuthenticationContext? = null,
)

/**
 * Authentication context for a specific platform.
 * Contains session cookies/tokens for authenticated extraction.
 */
data class AuthenticationContext(
    val platformId: String,
    val sessionId: String,
    val cookies: Map<String, String> = emptyMap(),
    val token: String? = null,
    val expiresAt: Long? = null,
)

/**
 * Result of a media resolution attempt.
 * Tied to the exact taskId from the request.
 */
data class ResolveResult(
    val taskId: String,
    val platformId: String,
    val contentId: String?,
    val status: ResolveStatus,
    val metadata: MediaMetadata? = null,
    val formats: List<MediaFormat> = emptyList(),
    val resolverEngine: String? = null,
    val error: ResolveError? = null,
    val errorMessage: String? = null,
    val diagnostics: ResolverDiagnostics? = null,
)

enum class ResolveStatus {
    RESOLVED,           // Successfully resolved — formats contain real download URLs
    AUTHENTICATION_REQUIRED,  // Platform requires login
    BACKEND_REQUIRED,   // Cannot resolve locally — needs remote extraction service
    UNSUPPORTED,        // Platform is not supported
    FAILED,             // Resolution failed with a specific error
}

enum class ResolveError {
    UNSUPPORTED_PLATFORM,
    UNSUPPORTED_CONTENT_TYPE,
    AUTHENTICATION_REQUIRED,
    PRIVATE_CONTENT,
    GEO_RESTRICTED,
    NO_MEDIA_STREAM,
    PLATFORM_CHANGED,
    EXTRACTION_ENGINE_FAILURE,
    BACKEND_UNAVAILABLE,
    NETWORK_FAILURE,
    RATE_LIMITED,
    MEDIA_URL_EXPIRED,
    SESSION_EXPIRED,
    DOWNLOAD_VERIFICATION_FAILED,
}

/**
 * Platform capability — reports what the resolver can actually do.
 * Do NOT report resolvable=true unless the resolver can actually resolve media.
 */
data class PlatformCapability(
    val platformId: String,
    val detectable: Boolean,
    val resolvable: Boolean,
    val downloadable: Boolean,
    val authRequired: Boolean,
    val authSupported: Boolean,
    val backendRequired: Boolean,
    val temporarilyUnavailable: Boolean,
)

/**
 * Internal diagnostics for each resolution attempt.
 * Does NOT contain sensitive data (no cookies, tokens, passwords).
 */
data class ResolverDiagnostics(
    val platform: String,
    val resolver: String,
    val engine: String,
    val taskId: String,
    val stage: String,
    val durationMs: Long,
    val status: String,
    val errorCode: String? = null,
)
