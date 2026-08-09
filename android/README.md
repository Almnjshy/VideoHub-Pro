# VideoHub Pro Enterprise — Native Android App

تطبيق Android أصلي بالكامل — مبني بـ **Kotlin + Jetpack Compose** (ليس WebView ولا PWA).

## ✨ الميزات

### 🏗️ بنية أصلية كاملة
- **Kotlin 2.1** + **Jetpack Compose** + **Material 3**
- **Hilt** للحقن (Dependency Injection)
- **Room** لقاعدة البيانات (SQLite)
- **WorkManager + Foreground Service** للتنزيل في الخلفية
- **Media3 (ExoPlayer)** لتشغيل الوسائط
- **Coil** لتحميل الصور
- **OkHttp + Retrofit** للشبكة

### 📱 ميزات Android الأصلية
- **Foreground Service** — تنزيل يستمر حتى بعد إغلاق التطبيق
- **Notification Channels** — 3 قنوات (تنزيلات، أحداث، وحدات)
- **Share Intent** — استقبال روابط من أي تطبيق (يوتيوب، متصفح، إلخ)
- **WakeLock** — منع النوم أثناء التنزيل
- **FileProvider** — مشاركة الملفات المنزّلة
- **Edge-to-Edge** + **Splash Screen**
- **Adaptive Icons** (مsupport AMOLED)

### 🔌 نظام الوحدات (Plugin System)
14 منصة مدعومة، كل واحدة كـ `PlatformPlugin` مستقلة:
- YouTube, TikTok, Facebook, X, Instagram
- Vimeo, Dailymotion, Reddit, Twitch
- SoundCloud, Pinterest, LinkedIn, Tumblr, Streamable

### 🎵 جودات متعددة
- **فيديو**: 360P, 480P, 720P, 1080P, 4K (60fps)
- **صوت**: MP3 128/192/320, M4A AAC 256, WAV Lossless

### 🧠 محرك تنزيل احترافي
- جدولة ذكية (priority + per-domain limit)
- تنزيل متعدد الأجزاء (4 segments)
- إعادة محاولة تلقائية
- حد النطاق الترددي
- مراقبة صحة الوحدات + اختبارات تلقائية

## 🏗️ البناء

### المتطلبات
- JDK 17
- Android SDK 35
- Gradle 8.9

### بناء Debug APK
```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### بناء Release APK
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

### بناء AAB (لـ Play Store)
```bash
./gradlew bundleRelease
# AAB: app/build/outputs/bundle/release/app-release.aab
```

## 📁 البنية

```
android/
├── app/
│   ├── build.gradle.kts              # إعدادات البناء
│   ├── proguard-rules.pro            # قواعد ProGuard
│   └── src/main/
│       ├── AndroidManifest.xml       # Manifest + Permissions
│       ├── java/com/videohub/pro/
│       │   ├── VideoHubApp.kt        # Application class (Hilt)
│       │   ├── MainActivity.kt       # Activity + NavHost
│       │   ├── di/AppModule.kt       # Hilt DI Module
│       │   ├── data/
│       │   │   ├── database/         # Room Database + DAOs
│       │   │   │   ├── VideoHubDatabase.kt
│       │   │   │   ├── dao/          # 6 DAOs (Task, Plugin, Fault, etc.)
│       │   │   │   └── entities/     # 6 Entities
│       │   │   └── ...
│       │   ├── domain/models/        # Domain models
│       │   ├── engine/
│       │   │   ├── DownloadEngine.kt # محرك التنزيل (Smart Scheduler)
│       │   │   └── DownloadService.kt# Foreground Service
│       │   ├── plugins/              # 14 وحدة منصة
│       │   │   ├── PlatformPlugin.kt # Interface
│       │   │   ├── PluginRegistry.kt # Registry
│       │   │   └── Plugins.kt        # 14 implementations
│       │   ├── notifications/        # Notification channels
│       │   ├── ui/
│       │   │   ├── theme/            # Colors, Theme, Typography
│       │   │   ├── navigation/       # Screen routes
│       │   │   ├── components/       # SmartShareOverlay, etc.
│       │   │   └── screens/          # 9 شاشات
│       │   └── ...
│       └── res/                      # Resources (strings, colors, themes)
├── build.gradle.kts                  # Root build file
├── settings.gradle.kts               # Project settings
├── gradle.properties                 # Gradle config
├── gradle/libs.versions.toml         # Version catalog
└── gradlew                           # Gradle wrapper
```

## 🚀 GitHub Actions

ملف `.github/workflows/android-build.yml` يقوم بـ:

1. **🔍 Lint Check** — فحص الكود
2. **🏗️ Build Debug APK** — للاختبار السريع
3. **🚀 Build Release APK** — APK موقّع للإنتاج
4. **📦 Build AAB** — Android App Bundle للـ Play Store
5. **🎉 Create GitHub Release** — عند دفع tag `v*`

### تشغيل الـ Workflow
```bash
# ارفع الكود إلى GitHub
git add android/ .github/
git commit -m "feat: native Android app"
git push origin main

# لإنشاء Release مع APK:
git tag v1.0.0
git push origin v1.0.0
```

### تنزيل الـ APK
بعد تشغيل الـ workflow:
1. اذهب إلى تبويب **Actions** في GitHub
2. اختر آخر تشغيل
3. نزّل `videohub-pro-debug-apk` أو `videohub-pro-release-apk`

## 📋 الأذونات المطلوبة

| الإذن | السبب |
|-------|-------|
| `INTERNET` | تنزيل الملفات |
| `ACCESS_NETWORK_STATE` | فحص الاتصال |
| `FOREGROUND_SERVICE` | تنزيل في الخلفية |
| `POST_NOTIFICATIONS` | إشعارات التنزيل |
| `READ_MEDIA_VIDEO/AUDIO/IMAGES` | الوصول للوسائط المنزّلة |
| `WAKE_LOCK` | منع النوم أثناء التنزيل |
| `VIBRATE` | اهتزاز للإشعارات |
| `READ_CLIPBOARD` | مراقب الحافظة |

## 🎨 الثيمات

- **Dark** — خلفية `#09090B` (افتراضي)
- **Light** — خلفية `#FAFAFA`
- **AMOLED** — خلفية سوداء نقية `#000000` (لتوفير البطارية)

اللون المميز: Amber `#F59E0B`
