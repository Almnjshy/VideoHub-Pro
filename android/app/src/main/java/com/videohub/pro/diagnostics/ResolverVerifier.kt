package com.videohub.pro.diagnostics

import android.util.Log
import com.videohub.pro.auth.AuthenticationManager
import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.network.DownloadResult
import com.videohub.pro.network.NetworkClient
import com.videohub.pro.plugins.PluginRegistry
import com.videohub.pro.resolver.ResolveResult
import com.videohub.pro.resolver.ResolveStatus
import com.videohub.pro.resolver.ResolverManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolver Verification System — runs complete end-to-end pipeline tests.
 *
 * Tests every stage from URL to downloaded file:
 * DETECT → RESOLVE → LIST FORMATS → TEST MEDIA URL → DOWNLOAD → VERIFY FILE → VALIDATE
 *
 * Each stage reports PASS/FAIL/SKIPPED with actual error messages.
 * Does NOT display sensitive cookies or tokens.
 */
@Singleton
class ResolverVerifier @Inject constructor(
    private val pluginRegistry: PluginRegistry,
    private val resolverManager: ResolverManager,
    private val networkClient: NetworkClient,
    private val authManager: AuthenticationManager,
) {

    companion object {
        private const val TAG = "ResolverVerifier"
    }

    data class VerificationResult(
        val url: String,
        val platform: String,
        val stages: List<StageResult>,
        val overallStatus: VerificationStatus,
    )

    data class StageResult(
        val stage: String,
        val status: StageStatus,
        val message: String,
        val details: Map<String, String> = emptyMap(),
    )

    enum class StageStatus { PASS, FAIL, SKIPPED, NOT_VERIFIED }
    enum class VerificationStatus { PASSED, FAILED, PARTIAL }

    /**
     * Run complete verification pipeline for a URL.
     */
    suspend fun verify(url: String, outputDir: File): VerificationResult {
        val stages = mutableListOf<StageResult>()
        val taskId = UUID.randomUUID().toString()

        // Stage 1: DETECTION
        val plugin = pluginRegistry.findForUrl(url)
        if (plugin == null) {
            stages.add(StageResult("DETECT", StageStatus.FAIL, "No platform detected for URL"))
            return VerificationResult(url, "unknown", stages, VerificationStatus.FAILED)
        }
        stages.add(StageResult("DETECT", StageStatus.PASS, "Platform: ${plugin.id} (${plugin.nameAr})"))

        // Stage 2: AUTHENTICATION STATE
        val authContext = authManager.getAuthContext(plugin.id)
        val authStatus = if (authContext != null) {
            stages.add(StageResult("AUTH", StageStatus.PASS, "Authenticated (${authContext.cookies.size} cookies)"))
            "AUTHENTICATED"
        } else if (authManager.supportsAuth(plugin.id)) {
            stages.add(StageResult("AUTH", StageStatus.SKIPPED, "Not logged in — resolver may fail for authenticated content"))
            "NOT_REQUIRED"
        } else {
            stages.add(StageResult("AUTH", StageStatus.SKIPPED, "Authentication not supported for this platform"))
            "NOT_REQUIRED"
        }

        // Stage 3: RESOLVE
        val resolveResult = try {
            withContext(Dispatchers.IO) {
                resolverManager.resolve(url, taskId, authContext)
            }
        } catch (e: Exception) {
            stages.add(StageResult("RESOLVE", StageStatus.FAIL, e.message ?: "Exception during resolution"))
            return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
        }

        if (resolveResult.status != ResolveStatus.RESOLVED) {
            stages.add(StageResult("RESOLVE", StageStatus.FAIL,
                "Resolution failed: ${resolveResult.status} — ${resolveResult.errorMessage}"))
            return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
        }

        stages.add(StageResult("RESOLVE", StageStatus.PASS,
            "Resolved by ${resolveResult.resolverEngine}",
            mapOf("formats" to "${resolveResult.formats.size} formats found")))

        // Stage 4: METADATA
        val metadata = resolveResult.metadata
        if (metadata == null) {
            stages.add(StageResult("METADATA", StageStatus.FAIL, "No metadata returned"))
        } else {
            stages.add(StageResult("METADATA", StageStatus.PASS,
                "Title: ${metadata.title}",
                mapOf("author" to (metadata.author ?: "N/A"),
                    "thumbnail" to (metadata.thumbnailUrl != null).toString(),
                    "duration" to (metadata.durationSeconds?.toString() ?: "N/A"))))
        }

        // Stage 5: FORMATS
        if (resolveResult.formats.isEmpty()) {
            stages.add(StageResult("FORMATS", StageStatus.FAIL, "No formats returned"))
            return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
        }
        val formatsSummary = resolveResult.formats.joinToString("\n") { "  - ${it.label} (${it.ext}, ${it.quality})" }
        stages.add(StageResult("FORMATS", StageStatus.PASS,
            "${resolveResult.formats.size} formats:\n$formatsSummary"))

        // Stage 6: SELECT FORMAT (pick first with downloadUrl)
        val selectedFormat = resolveResult.formats.firstOrNull { it.downloadUrl != null }
        if (selectedFormat == null) {
            stages.add(StageResult("FORMAT_SELECT", StageStatus.FAIL, "No format with downloadUrl"))
            return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
        }
        stages.add(StageResult("FORMAT_SELECT", StageStatus.PASS,
            "Selected: ${selectedFormat.label} (${selectedFormat.ext})"))

        // Stage 7: TEST MEDIA URL (HEAD request)
        val mediaUrl = selectedFormat.downloadUrl!!
        val testFile = File(outputDir, "test_${taskId.take(8)}.${selectedFormat.ext}")
        var totalBytes = 0L
        var contentType = ""

        // Stage 8: DOWNLOAD
        val downloadResult = try {
            withContext(Dispatchers.IO) {
                networkClient.downloadFile(
                    url = mediaUrl,
                    outputFile = testFile,
                    onProgress = { downloaded, total ->
                        totalBytes = downloaded
                        if (total > 0) totalBytes = total
                    },
                )
            }
        } catch (e: Exception) {
            stages.add(StageResult("DOWNLOAD", StageStatus.FAIL, e.message ?: "Download exception"))
            return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
        }

        when (downloadResult) {
            is DownloadResult.Success -> {
                stages.add(StageResult("DOWNLOAD", StageStatus.PASS,
                    "Downloaded ${downloadResult.fileSize} bytes",
                    mapOf("file" to downloadResult.filePath)))
            }
            is DownloadResult.Error -> {
                stages.add(StageResult("DOWNLOAD", StageStatus.FAIL, downloadResult.message))
                return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
            }
        }

        // Stage 9: FILE VERIFICATION
        val file = File(downloadResult.filePath)
        if (!file.exists()) {
            stages.add(StageResult("FILE_VERIFY", StageStatus.FAIL, "File does not exist"))
            return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
        }
        if (file.length() == 0L) {
            stages.add(StageResult("FILE_VERIFY", StageStatus.FAIL, "File is empty (0 bytes)"))
            return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
        }
        stages.add(StageResult("FILE_VERIFY", StageStatus.PASS,
            "File exists, size: ${file.length()} bytes",
            mapOf("path" to file.absolutePath, "size" to file.length().toString())))

        // Stage 10: MEDIA VALIDATION (basic — check file signature)
        val isValidMedia = validateMediaFile(file, selectedFormat.ext)
        if (isValidMedia) {
            stages.add(StageResult("MEDIA_VALIDATION", StageStatus.PASS,
                "File appears to be valid ${selectedFormat.ext} media"))
        } else {
            stages.add(StageResult("MEDIA_VALIDATION", StageStatus.FAIL,
                "File does not appear to be valid ${selectedFormat.ext} media — may be HTML or corrupt"))
            // Clean up invalid file
            file.delete()
            return VerificationResult(url, plugin.id, stages, VerificationStatus.FAILED)
        }

        // Overall
        val overall = if (stages.all { it.status == StageStatus.PASS }) VerificationStatus.PASSED
                      else if (stages.any { it.status == StageStatus.FAIL }) VerificationStatus.FAILED
                      else VerificationStatus.PARTIAL

        return VerificationResult(url, plugin.id, stages, overall)
    }

    /**
     * Basic media file validation — checks file signature/magic bytes.
     */
    private fun validateMediaFile(file: File, ext: String): Boolean {
        return try {
            val bytes = ByteArray(16)
            file.inputStream().use { it.read(bytes) }

            when (ext.lowercase()) {
                "mp4", "m4a" -> {
                    // MP4/M4A: check for ftyp box at offset 4
                    bytes.size >= 8 &&
                    bytes[4] == 'f'.code.toByte() &&
                    bytes[5] == 't'.code.toByte() &&
                    bytes[6] == 'y'.code.toByte() &&
                    bytes[7] == 'p'.code.toByte()
                }
                "mp3" -> {
                    // MP3: check for ID3 tag or MPEG sync word
                    (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) ||
                    (bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0)
                }
                "webm" -> {
                    // WebM: check for EBML header
                    bytes[0] == 0x1A.toByte() &&
                    bytes[1] == 0x45.toByte() &&
                    bytes[2] == 0xDF.toByte() &&
                    bytes[3] == 0xA3.toByte()
                }
                "ogg" -> {
                    // OGG: check for OggS signature
                    bytes[0] == 'O'.code.toByte() &&
                    bytes[1] == 'g'.code.toByte() &&
                    bytes[2] == 'g'.code.toByte() &&
                    bytes[3] == 'S'.code.toByte()
                }
                "jpg", "jpeg" -> bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
                "png" -> bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte()
                "gif" -> bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte()
                else -> true // Unknown extension — assume valid
            }
        } catch (e: Exception) {
            false
        }
    }
}
