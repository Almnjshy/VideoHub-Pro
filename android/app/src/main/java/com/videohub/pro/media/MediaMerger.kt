package com.videohub.pro.media

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaMerger — Graceful no-op fallback (FFmpeg-free).
 *
 * NOTE: The arthenica/ffmpeg-kit project was archived in April 2025 and its
 * Maven artifacts (`com.arthenica:ffmpeg-kit-full:6.0-2`) are no longer
 * resolvable from Maven Central or Google Maven. To keep the build green,
 * this class is a no-op stub that returns [MergeResult.NotAvailable] for
 * every operation. All real download / extraction still works because:
 *
 *   1. yt-dlp (embedded via Chaquopy) extracts the media URLs.
 *   2. resolver.py prefers pre-merged "video+audio" formats so most
 *      downloads land as a single ready-to-play file.
 *   3. The DownloadEngine streams directly from the URL to disk using
 *      OkHttp — it does not depend on FFmpeg at all.
 *
 * Future work: replace this stub with a community fork of ffmpeg-kit
 * (e.g. a JitPack-hosted fork) or ship a prebuilt FFmpeg binary via NDK.
 */
@Singleton
class MediaMerger @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "MediaMerger"
        private const val NOT_AVAILABLE_MSG =
            "FFmpeg not available — install a build with FFmpeg support to use merge/extract/trim/compress."
    }

    /**
     * Always returns false in this build. Kept for API compatibility with
     * callers that may probe the runtime before invoking operations.
     */
    fun isFFmpegAvailable(): Boolean {
        Log.i(TAG, "isFFmpegAvailable() = false (no-op build)")
        return false
    }

    /**
     * Merge a video-only file with an audio-only file.
     * Returns [MergeResult.NotAvailable] in this build.
     */
    suspend fun mergeVideoAudio(
        videoFile: File,
        audioFile: File,
        outputFile: File,
        onProgress: ((Int) -> Unit)? = null,
    ): MergeResult {
        Log.w(TAG, "mergeVideoAudio() called but FFmpeg is not bundled in this build")
        return MergeResult.NotAvailable(NOT_AVAILABLE_MSG)
    }

    /**
     * Extract audio from a video file.
     * Returns [MergeResult.NotAvailable] in this build.
     */
    suspend fun extractAudio(
        videoFile: File,
        outputFile: File,
    ): MergeResult {
        Log.w(TAG, "extractAudio() called but FFmpeg is not bundled in this build")
        return MergeResult.NotAvailable(NOT_AVAILABLE_MSG)
    }

    /**
     * Trim/cut a media file to a specific time range.
     * Returns [MergeResult.NotAvailable] in this build.
     */
    suspend fun trimMedia(
        inputFile: File,
        outputFile: File,
        startSeconds: Double,
        durationSeconds: Double?,
    ): MergeResult {
        Log.w(TAG, "trimMedia() called but FFmpeg is not bundled in this build")
        return MergeResult.NotAvailable(NOT_AVAILABLE_MSG)
    }

    /**
     * Compress a video file to reduce its size.
     * Returns [MergeResult.NotAvailable] in this build.
     */
    suspend fun compressVideo(
        inputFile: File,
        outputFile: File,
        crf: Int = 28,
    ): MergeResult {
        Log.w(TAG, "compressVideo() called but FFmpeg is not bundled in this build")
        return MergeResult.NotAvailable(NOT_AVAILABLE_MSG)
    }
}

/**
 * Result of a merge/extract/trim/compress operation.
 */
sealed class MergeResult {
    data class Success(val outputFile: File) : MergeResult()
    data class Error(val message: String) : MergeResult()
    data class NotAvailable(val message: String) : MergeResult()
}
