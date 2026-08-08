package com.videohub.pro

import android.app.Application
import com.videohub.pro.diagnostics.CrashReporter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VideoHubApp : Application() {

    override fun onCreate() {
        // Must be first — install crash handler before anything else can throw
        CrashReporter.install(this)
        super.onCreate()
        // NOTE: DownloadService is NOT started here.
        // It is started only when a real download task is created (via ShareViewModel.startDownload).
        // This prevents the permanent "Downloading..." notification when queue is empty.
        // NOTE: WorkManager Configuration.Provider removed — we don't use @HiltWorker.
        // This also removes the transitive dependency on old Dagger/JavaPoet that caused
        // the hiltAggregateDepsDebug NoSuchMethodError.
    }
}
