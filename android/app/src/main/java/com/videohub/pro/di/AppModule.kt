package com.videohub.pro.di

import android.content.Context
import androidx.room.Room
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.plugins.PluginRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module — provides singletons that CANNOT use @Inject constructor.
 *
 * All other classes (NetworkClient, MetadataExtractor, YtDlpResolver,
 * ResolverManager, SearchEngine, NotificationHelper, DownloadEngine,
 * SecureSessionStorage, AuthenticationManager, AppSettings, MediaMerger,
 * ResolverVerifier) have @Inject constructor + @Singleton annotation,
 * so Hilt provides them automatically — no @Provides needed here.
 *
 * Only VideoHubDatabase (Room builder) and PluginRegistry (no @Inject)
 * need explicit @Provides methods.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VideoHubDatabase {
        return Room.databaseBuilder(
            context,
            VideoHubDatabase::class.java,
            VideoHubDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePluginRegistry(): PluginRegistry = PluginRegistry()
}
