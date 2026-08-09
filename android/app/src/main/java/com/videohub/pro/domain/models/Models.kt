package com.videohub.pro.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType { VIDEO, AUDIO, IMAGE, FILE }

@Serializable
enum class MediaQuality(val label: String) {
    AUDIO("audio"), P144("144p"), P240("240p"), P360("360p"),
    P480("480p"), P720("720p"), P1080("1080p"), P4K("4k"),
}

@Serializable
data class MediaFormat(
    val id: String,
    val quality: MediaQuality,
    val ext: String,
    val sizeBytes: Long,
    val mediaType: MediaType,
    val hasAudio: Boolean = true,
    val bitrate: Int? = null,
    val fps: Int? = null,
    val label: String,
    val downloadUrl: String? = null, // REAL URL to download the actual media file
)

@Serializable
data class MediaMetadata(
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val description: String? = null,
    val sourceUrl: String,
    val platformId: String,
    val formats: List<MediaFormat>,
    val resolvedAt: Long,
)

enum class TaskStatus(val label: String) {
    QUEUED("queued"), RESOLVING("resolving"), DOWNLOADING("downloading"),
    PAUSED("paused"), COMPLETED("completed"), FAILED("failed"), RETRYING("retrying"),
}

enum class TaskPriority(val value: Int, val label: String) {
    HIGH(0, "عالية"), NORMAL(1, "عادية"), LOW(2, "منخفضة");

    companion object {
        fun fromValue(v: Int): TaskPriority = entries.find { it.value == v } ?: NORMAL
    }
}

enum class ErrorStage {
    IDENTIFY, FETCH_METADATA, RESOLVE_LINKS, DOWNLOAD_SEGMENT, MERGE, FINALIZE
}

enum class PluginErrorType {
    NETWORK, PARSING, AUTH, SOURCE_CHANGED, RATE_LIMIT, UNKNOWN
}

enum class PluginHealthStatus { HEALTHY, DEGRADED, BROKEN }

data class PluginError(
    val pluginId: String,
    val stage: ErrorStage,
    val errorType: PluginErrorType,
    val message: String,
    val timestamp: Long,
    val contentId: String? = null,
)

data class PluginHealth(
    val pluginId: String,
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val failedAttempts: Int,
    val successRate: Float,
    val lastError: PluginError? = null,
    val lastSuccessAt: Long? = null,
    val status: PluginHealthStatus,
    val lastAutoTestAt: Long? = null,
    val nextAutoTestAt: Long? = null,
)

data class FaultReport(
    val id: String,
    val pluginId: String,
    val stage: ErrorStage,
    val errorType: PluginErrorType,
    val message: String,
    val timestamp: Long,
    val taskId: String? = null,
    val resolved: Boolean = false,
)

data class AutoTestResult(
    val pluginId: String,
    val testName: String,
    val passed: Boolean,
    val durationMs: Long,
    val timestamp: Long,
    val error: String? = null,
)
