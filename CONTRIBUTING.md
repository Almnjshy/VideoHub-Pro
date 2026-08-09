# Contributing to VideoHub Pro

شكراً لاهتمامك بالمساهمة في VideoHub Pro! هذا الدليل يوضح كيفية المساهمة.

## 🚀 كيفية البدء

### المتطلبات
- Android Studio (Hedgehog أو أحدث)
- Python 3.11+ (للـ Chaquopy build)
- FFmpeg (مثبت على النظام للبناء)
- JDK 17
- Git

### إعداد البيئة
```bash
# Clone the repo
git clone https://github.com/<your-username>/VideoHub-Pro.git
cd VideoHub-Pro

# Install web dependencies (optional, for web app)
bun install

# Build Android debug APK
cd android
./gradlew assembleDebug
```

## 📝 قواعد المساهمة

### 1. Branches
- `main` — الإصدار المستقر
- `dev` — التطوير النشط
- `feature/<name>` — ميزة جديدة
- `fix/<name>` — إصلاح خطأ

### 2. Commits
استخدم صيغة واضحة:
```
feat: add clipboard monitor
fix: resolve status bar overlap
docs: update README
refactor: simplify resolver.py
```

### 3. Code Style
- **Kotlin**: اتبع [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Python**: اتبع [PEP 8](https://peps.python.org/pep-0008/)
- **TypeScript**: اتبع ESLint config الموجود

### 4. Testing
- اختبر على جهاز حقيقي قبل الإرسال
- تأكد أن البناء ينجح: `./gradlew assembleDebug`
- تأكد أن lint يمر: `bun run lint`

### 5. Pull Requests
- صف التغيير بوضوح
- أرفق screenshots إذا كان هناك تغيير بصري
- اربط الـ PR بالـ issue المناسب

## 🏗️ البنية المعمارية

```
URL → PluginRegistry (detection)
    → ResolverManager → YtDlpResolver → Chaquopy → Python → yt-dlp
    → Real Metadata + Formats + Download URLs
    → DownloadEngine → NetworkClient → Real file on disk
    → Notification (progress, speed, ETA)
```

## ⚠️ قواعد مهمة

1. **لا تتجاوز DRM/CAPTCHA/MFA** — yt-dlp يستخدم cookies بشكل شرعي فقط
2. **لا تحذف Chaquopy** — المحرك يعتمد عليه
3. **لا تعدل resolver.py الأساسي** — أضف functions جديدة في النهاية
4. **لا تستخدم بيانات وهمية** — كل البيانات يجب أن تكون حقيقية من DB أو الجهاز

## 📞 التواصل

- Issues: [GitHub Issues](https://github.com/<your-username>/VideoHub-Pro/issues)
- Discussions: [GitHub Discussions](https://github.com/<your-username>/VideoHub-Pro/discussions)

## 📄 الترخيص

بالمساهمة، أنت توافق على أن مساهمتك ستُرخص تحت رخصة MIT.
