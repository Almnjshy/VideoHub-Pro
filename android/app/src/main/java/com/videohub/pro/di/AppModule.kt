package com.videohub.pro.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.videohub.pro.data.database.VideoHubDatabase
import com.videohub.pro.engine.DownloadEngine
import com.videohub.pro.auth.AuthenticationManager
import com.videohub.pro.auth.SecureSessionStorage
import com.videohub.pro.diagnostics.ResolverVerifier
import com.videohub.pro.media.MediaMerger
import com.videohub.pro.network.MetadataExtractor
import com.videohub.pro.network.NetworkClient
import com.videohub.pro.network.SearchEngine
import com.videohub.pro.notifications.NotificationHelper
import com.videohub.pro.plugins.PluginRegistry
import com.videohub.pro.resolver.FacebookResolver
import com.videohub.pro.resolver.InstagramResolver
import com.videohub.pro.resolver.LinkedInResolver
import com.videohub.pro.resolver.MediaResolver
import com.videohub.pro.resolver.PinterestResolver
import com.videohub.pro.resolver.RedditResolver
import com.videohub.pro.resolver.ResolverManager
import com.videohub.pro.resolver.SoundCloudResolver
import com.videohub.pro.resolver.TikTokResolver
import com.videohub.pro.resolver.TumblrResolver
import com.videohub.pro.resolver.TwitchResolver
import com.videohub.pro.resolver.TwitterResolver
import com.videohub.pro.resolver.YouTubeResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    @Provides
    @Singleton
    fun provideNetworkClient(): NetworkClient = NetworkClient()

    @Provides
    @Singleton
    fun provideMetadataExtractor(networkClient: NetworkClient): MetadataExtractor =
        MetadataExtractor(networkClient)

    @Provides
    @Singleton
    fun provideMediaResolver(networkClient: NetworkClient): MediaResolver =
        MediaResolver(networkClient)

    // ============ Platform-specific Resolvers ============
    @Provides @Singleton fun provideYouTubeResolver(nc: NetworkClient) = YouTubeResolver(nc)
    @Provides @Singleton fun provideFacebookResolver(nc: NetworkClient) = FacebookResolver(nc)
    @Provides @Singleton fun provideTikTokResolver(nc: NetworkClient) = TikTokResolver(nc)
    @Provides @Singleton fun provideInstagramResolver(nc: NetworkClient) = InstagramResolver(nc)
    @Provides @Singleton fun provideTwitterResolver(nc: NetworkClient) = TwitterResolver(nc)
    @Provides @Singleton fun provideRedditResolver(nc: NetworkClient) = RedditResolver(nc)
    @Provides @Singleton fun provideTwitchResolver(nc: NetworkClient) = TwitchResolver(nc)
    @Provides @Singleton fun provideSoundCloudResolver(nc: NetworkClient) = SoundCloudResolver(nc)
    @Provides @Singleton fun providePinterestResolver(nc: NetworkClient) = PinterestResolver(nc)
    @Provides @Singleton fun provideLinkedInResolver(nc: NetworkClient) = LinkedInResolver(nc)
    @Provides @Singleton fun provideTumblrResolver(nc: NetworkClient) = TumblrResolver(nc)

    @Provides
    @Singleton
    fun provideResolverManager(
        pluginRegistry: PluginRegistry,
        networkClient: NetworkClient,
        youTubeResolver: YouTubeResolver,
        facebookResolver: FacebookResolver,
        tiktokResolver: TikTokResolver,
        instagramResolver: InstagramResolver,
        twitterResolver: TwitterResolver,
        redditResolver: RedditResolver,
        twitchResolver: TwitchResolver,
        soundCloudResolver: SoundCloudResolver,
        pinterestResolver: PinterestResolver,
        linkedInResolver: LinkedInResolver,
        tumblrResolver: TumblrResolver,
        mediaResolver: MediaResolver,
    ): ResolverManager = ResolverManager(
        pluginRegistry, networkClient,
        youTubeResolver, facebookResolver, tiktokResolver, instagramResolver,
        twitterResolver, redditResolver, twitchResolver, soundCloudResolver,
        pinterestResolver, linkedInResolver, tumblrResolver, mediaResolver,
    )

    @Provides
    @Singleton
    fun provideSearchEngine(networkClient: NetworkClient): SearchEngine =
        SearchEngine(networkClient)

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
        NotificationHelper(context)

    @Provides
    @Singleton
    fun provideDownloadEngine(
        database: VideoHubDatabase,
        notificationHelper: NotificationHelper,
        @ApplicationContext context: Context,
        networkClient: NetworkClient,
    ): DownloadEngine = DownloadEngine(database, notificationHelper, context, networkClient)

    // ============ Auth + Security ============
    @Provides @Singleton fun provideSecureSessionStorage(@ApplicationContext ctx: Context) = SecureSessionStorage(ctx)
    @Provides @Singleton fun provideAuthenticationManager(storage: SecureSessionStorage) = AuthenticationManager(storage)

    // ============ Media ============
    @Provides @Singleton fun provideMediaMerger(@ApplicationContext ctx: Context) = MediaMerger(ctx)

    // ============ Diagnostics ============
    @Provides @Singleton
    fun provideResolverVerifier(
        pluginRegistry: PluginRegistry,
        resolverManager: ResolverManager,
        networkClient: NetworkClient,
        authManager: AuthenticationManager,
    ): ResolverVerifier = ResolverVerifier(pluginRegistry, resolverManager, networkClient, authManager)
}
