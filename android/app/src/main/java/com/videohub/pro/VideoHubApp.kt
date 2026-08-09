package com.videohub.pro

import android.app.Application
import android.util.Log
import com.videohub.pro.diagnostics.CrashReporter
import com.videohub.pro.resolver.ResolverManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class VideoHubApp : Application() {

    @Inject
    lateinit var resolverManager: ResolverManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        // Must be first — install crash handler before anything else can throw
        CrashReporter.install(this)
        super.onCreate()

        // Initialize embedded Python + yt-dlp in background.
        // This takes ~2-3 seconds on first run, then is instant on subsequent runs.
        appScope.launch {
            try {
                resolverManager.initialize()
                Log.i("VideoHubApp", "yt-dlp ready: ${resolverManager.getVersion()}")
            } catch (e: Exception) {
                Log.e("VideoHubApp", "Failed to initialize yt-dlp", e)
            }
        }
    }
}
