package com.videohub.pro.ui.components

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.data.database.entities.NotificationEntity
import com.videohub.pro.data.database.entities.TaskEntity
import com.videohub.pro.domain.models.MediaFormat
import com.videohub.pro.domain.models.MediaMetadata
import com.videohub.pro.domain.models.TaskStatus
import com.videohub.pro.engine.DownloadEngine
import com.videohub.pro.engine.DownloadService
import com.videohub.pro.plugins.PlatformPlugin
import com.videohub.pro.plugins.PluginRegistry
import com.videohub.pro.resolver.AuthenticationContext
import com.videohub.pro.resolver.ResolveResult
import com.videohub.pro.resolver.ResolveStatus
import com.videohub.pro.resolver.ResolverManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class ShareState(
    val isLoading: Boolean = false,
    val detectedPlugin: PlatformPlugin? = null,
    val metadata: MediaMetadata? = null,
    val formats: List<MediaFormat> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val pluginRegistry: PluginRegistry,
    private val database: VideoHubDatabase,
    private val downloadEngine: DownloadEngine,
    private val resolverManager: ResolverManager,
    private val app: Application,
) : ViewModel() {

    private val _state = MutableStateFlow(ShareState())
    val state: StateFlow<ShareState> = _state.asStateFlow()

    /**
     * Resolve a shared URL using the REAL ResolverManager.
     * Each call generates a unique taskId — no cross-task contamination.
     */
    fun resolveUrl(url: String) {
        val plugin = pluginRegistry.findForUrl(url)
        if (plugin == null) {
            _state.value = ShareState(error = "لا توجد منصة مدعومة لهذا الرابط")
            return
        }

        // Reset state completely — no stale data
        _state.value = ShareState(
            isLoading = true,
            detectedPlugin = plugin,
        )

        // Generate unique taskId for this resolution
        val taskId = UUID.randomUUID().toString()

        viewModelScope.launch {
            ensurePluginInDb(plugin)

            try {
                val result = withContext(Dispatchers.IO) {
                    resolverManager.resolve(url, taskId, authContext = null)
                }

                when (result.status) {
                    ResolveStatus.RESOLVED -> {
                        _state.value = ShareState(
                            isLoading = false,
                            detectedPlugin = plugin,
                            metadata = result.metadata,
                            formats = result.formats,
                            error = null,
                        )
                        database.pluginDao().incrementAttempts(plugin.id)
                        database.pluginDao().incrementSuccess(plugin.id, System.currentTimeMillis())
                        recomputeHealth(plugin.id)
                    }
                    ResolveStatus.AUTHENTICATION_REQUIRED -> {
                        _state.value = ShareState(
                            isLoading = false,
                            detectedPlugin = plugin,
                            error = result.errorMessage ?: "هذه المنصة تتطلب تسجيل الدخول",
                        )
                    }
                    ResolveStatus.BACKEND_REQUIRED -> {
                        _state.value = ShareState(
                            isLoading = false,
                            detectedPlugin = plugin,
                            error = result.errorMessage ?: "هذه المنصة تتطلب خدمة خلفية",
                        )
                    }
                    ResolveStatus.UNSUPPORTED -> {
                        _state.value = ShareState(
                            isLoading = false,
                            detectedPlugin = plugin,
                            error = result.errorMessage ?: "منصة غير مدعومة",
                        )
                    }
                    ResolveStatus.FAILED -> {
                        _state.value = ShareState(
                            isLoading = false,
                            detectedPlugin = plugin,
                            error = result.errorMessage ?: "فشل في استخراج الوسائط",
                        )
                        database.pluginDao().incrementAttempts(plugin.id)
                        database.pluginDao().incrementFailed(
                            plugin.id,
                            """{"pluginId":"${plugin.id}","stage":"resolve","errorType":"${result.error}","message":"${result.errorMessage}","timestamp":${System.currentTimeMillis()}}""",
                        )
                        recomputeHealth(plugin.id)
                    }
                }
            } catch (e: Exception) {
                _state.value = ShareState(
                    isLoading = false,
                    detectedPlugin = plugin,
                    error = e.message ?: "خطأ غير معروف",
                )
            }
        }
    }

    /**
     * Start a real download with the resolved format's downloadUrl.
     */
    fun startDownload(url: String, format: MediaFormat, priority: Int) {
        val metadata = _state.value.metadata ?: return
        val pluginId = metadata.platformId
        val downloadUrl = format.downloadUrl
            ?: return // No download URL — cannot start

        viewModelScope.launch {
            val segmentsJson = downloadEngine.buildInitialSegments(format.sizeBytes)
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                sourceUrl = url,
                platformId = pluginId,
                title = metadata.title,
                author = metadata.author,
                thumbnailUrl = metadata.thumbnailUrl,
                durationSeconds = metadata.durationSeconds,
                description = metadata.description,
                formatId = format.id,
                formatQuality = format.quality.label,
                formatExt = format.ext,
                formatSizeBytes = format.sizeBytes,
                formatMediaType = format.mediaType.name.lowercase(),
                status = TaskStatus.QUEUED.name.lowercase(),
                totalBytes = format.sizeBytes,
                priority = priority,
                segmentsJson = segmentsJson,
                downloadUrl = downloadUrl,
            )
            database.taskDao().insert(task)
            ensureAppStats()

            database.notificationDao().insert(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    type = "task_started",
                    title = "تم بدء التنزيل",
                    message = metadata.title,
                    taskId = task.id,
                    timestamp = System.currentTimeMillis(),
                ),
            )

            DownloadService.start(app)
        }
    }

    private suspend fun ensurePluginInDb(plugin: PlatformPlugin) {
        if (database.pluginDao().getById(plugin.id) == null) {
            database.pluginDao().insert(
                com.videohub.pro.data.database.entities.PluginEntity(
                    id = plugin.id,
                    name = plugin.name,
                    nameAr = plugin.nameAr,
                    icon = plugin.icon,
                    color = plugin.color,
                    version = plugin.version,
                    enabled = true,
                ),
            )
        }
    }

    private suspend fun ensureAppStats() {
        if (database.appStatDao().get() == null) {
            database.appStatDao().insert(
                com.videohub.pro.data.database.entities.AppStatEntity(),
            )
        }
    }

    private suspend fun recomputeHealth(pluginId: String) {
        val plugin = database.pluginDao().getById(pluginId) ?: return
        val rate = if (plugin.totalAttempts > 0) plugin.successfulAttempts.toFloat() / plugin.totalAttempts else 1f
        val status = when {
            rate > 0.95 -> "healthy"
            rate > 0.80 -> "degraded"
            else -> "broken"
        }
        database.pluginDao().updateHealth(pluginId, rate, status)
    }
}
