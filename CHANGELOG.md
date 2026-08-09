# Changelog

All notable changes to VideoHub Pro will be documented in this file.

## [3.2.1] — 2026-08-09

### Fixed
- **Build failure** caused by removed `com.arthenica:ffmpeg-kit-full:6.0-2` dependency
  - The arthenica/ffmpeg-kit project was archived in April 2025 and its
    Maven artifacts are no longer resolvable from Maven Central or Google
    Maven, causing `:app:checkDebugAarMetadata` to fail.
  - Removed the dependency from `app/build.gradle.kts`.
  - `MediaMerger` is now a graceful no-op stub that returns
    `MergeResult.NotAvailable` for merge/extract/trim/compress operations.
  - `resolver.py` now surfaces combined (video+audio) formats first so
    downloads "just work" without needing an FFmpeg merge step on-device.
  - Future work: replace stub with a community-maintained FFmpeg fork
    (e.g. via JitPack) or ship a prebuilt FFmpeg binary via NDK.

## [3.2.0] — 2026-08-09

### Added
- **Embedded yt-dlp engine** via Chaquopy 17.0.0 + Python 3.11
  - Supports 1000+ platforms (YouTube, TikTok, Facebook, Instagram, X/Twitter, Vimeo, Dailymotion, Reddit, Twitch, SoundCloud, Pinterest, LinkedIn, Tumblr, Streamable, and more)
  - Real media extraction with real download URLs
  - Platform-specific cookie domains for authenticated content
  - Error classification with Arabic user messages
- **FFmpeg integration** (ffmpeg-kit-full 6.0-2) — REMOVED in 3.2.1 (archived upstream)
  - Video+audio merging for separate streams (1080p/4K)
  - Audio extraction from video files
  - Video trimming/cutting
  - Video compression
- **Clipboard monitor** — auto-detects copied URLs and opens download dialog
- **Search & Discover** — real YouTube search with filters
  - Trending by category (Now, Music, Gaming, Movies, News) and region (12 countries)
  - Multi-platform search (YouTube, YouTube Music, SoundCloud)
  - Search filters: sort by (relevance, views, date, rating), duration, time
  - Search suggestions (YouTube autocomplete)
  - Search history (saved in Room DB)
- **Media Workspace** — post-download tools
  - Play, Share, Extract Audio, Trim, Compress, File Info
- **Favorites** — save videos from Discover/Search (Room DB)
- **Library** — browse real downloaded files with play/share/delete
- **Download Scheduler** — schedule downloads via WorkManager (Wi-Fi only)
- **Thumbnail Cache** — LruCache + disk cache for fast thumbnail loading
- **Download Resume** — HTTP Range header for resuming interrupted downloads
- **9 Languages** — Arabic, English, French, Spanish, Portuguese, Russian, Chinese, Japanese, German
- **3 Themes** — Dark, Light, AMOLED (true black)
- **6 Accent Colors** — customizable accent
- **Real storage stats** — StatFs for actual device storage
- **Real notifications** — foreground service with progress, speed, ETA
- **Diagnostics screen** — real engine tests (Python, yt-dlp, YouTube resolve, network)
- **Pull-to-refresh** on Discover
- **Batch download** — resolve and download multiple URLs
- **Playlist support** — resolve YouTube playlists

### Changed
- Settings now persist to SharedPreferences (was volatile)
- DownloadEngine reads concurrent downloads and auto-retry from AppSettings
- DownloadsScreen shows real progress, speed, timestamps, and action buttons
- StatsScreen reads from real database (was hardcoded)
- HomeScreen shows real storage info via StatFs
- PluginsScreen reads from real pluginDao
- NotificationsScreen reads from real notificationDao

### Fixed
- Status bar overlap (contentWindowInsets fix)
- Chaquopy plugin resolution (Maven Central, not chaquo.com/maven)
- Kotlin DSL compatibility with Chaquopy 17
- KSP race condition (org.gradle.parallel=false)
- OkHttp deprecated API (toMediaType/toRequestBody)
- Prisma race condition (upsert instead of createMany)
- ESLint set-state-in-effect error
- Build script path resolution (relative, not absolute)

## [3.1.0] — 2026-07-31

### Added
- Initial Android app (Kotlin + Jetpack Compose)
- 14 platform plugins with URL detection
- Share intent receiver (ACTION_SEND)
- Room database with Task, Plugin, Notification, AppStat entities
- Hilt dependency injection
- Navigation with bottom bar
- Smart Share Overlay (bottom sheet)
- Media Player (ExoPlayer/Media3)
- Dark theme

## [3.0.0] — 2026-07-31

### Added
- Next.js web app with TypeScript + Tailwind
- Prisma SQLite database
- Web UI with 14 platform plugins
- Dashboard with KPIs, storage, active downloads
- Download engine (server-side, simulated progress)
