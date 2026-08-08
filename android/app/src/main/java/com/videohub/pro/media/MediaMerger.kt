package com.videohub.pro.media

import android.content.Context
import android.util.Log
import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media Merger — handles merging separate video and audio streams.
 *
 * When a platform (like YouTube) returns video-only and audio-only streams,
 * this class merges them into a single playable file.
 *
 * FFmpeg Integration:
 * - Currently NOT available — requires adding ffmpeg-kit dependency
 * - If not available, reports MERGE_NOT_AVAILABLE
 * - Does NOT fake merging
 */
@Singleton
class MediaMerger @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "MediaMerger"
    }

    fun isFFmpegAvailable(): Boolean {
        // FFmpeg is not currently integrated.
        // To add: implementation 'com.arthenica:ffmpeg-kit-full:6.0-2'
        return false
    }

    fun mergeVideoAudio(videoFile: File, audioFile: File, outputFile: File): MergeResult {
        if (!isFFmpegAvailable()) {
            return MergeResult.NotAvailable(
                "FFmpeg is not integrated. Add ffmpeg-kit dependency to enable video+audio merging. " +
                    "Without FFmpeg, only combined streams (video+audio in one file) can be downloaded."
            )
        }
        return MergeResult.NotAvailable("FFmpeg integration pending")
    }

    fun needsMerging(format: MediaFormat): Boolean {
        return format.mediaType == MediaType.VIDEO && !format.hasAudio
    }

    fun findMatchingAudio(videoFormat: MediaFormat, allFormats: List<MediaFormat>): MediaFormat? {
        return allFormats
            .filter { it.mediaType == MediaType.AUDIO }
            .maxByOrNull { it.bitrate ?: 0 }
    }
}

sealed class MergeResult {
    data class Success(val outputFile: File) : MergeResult()
    data class NotAvailable(val message: String) : MergeResult()
    data class Error(val message: String) : MergeResult()
}
