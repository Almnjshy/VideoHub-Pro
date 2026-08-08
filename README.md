# VideoHub Pro Enterprise

تطبيق احترافي لإدارة وتنزيل المحتوى الرقمي — منصة متكاملة لإدارة الوسائط.

## ✨ الميزات الرئيسية

- 📱 **تصميم Mobile-first** — متجاوب مع الجوال والتابلت (Bottom Navigation + Device Frame)
- 🔌 **14 منصة مدعومة** — YouTube, TikTok, Facebook, X, Instagram, Vimeo, Dailymotion, Reddit, Twitch, SoundCloud, Pinterest, LinkedIn, Tumblr, Streamable
- 🧠 **محرك تنزيل احترافي** — أولويات + جدولة ذكية + حد لكل منصة + حد النطاق الترددي
- 🎬 **مشغل وسائط مدمج** — تحكم بالسرعة + Picture-in-Picture
- 🛠️ **مساحة عمل الوسائط** — تحويل/استخراج صوت/قص/ضغط/مشاركة
- 📋 **مراقب الحافظة** — اكتشاف الروابط المنسوخة تلقائياً
- 🔍 **بحث عبر المنصات** + قسم اكتشف + مكتبة وسائط
- 🎨 **3 ثيمات** — داكنة / فاتحة / AMOLED
- 🌐 **RTL كامل** + دعم عربي/إنجليزي

## 🚀 التشغيل المحلي

```bash
# تثبيت الاعتماديات
bun install

# تهيئة قاعدة البيانات
bun run db:push

# تشغيل خادم التطوير
bun run dev

# فتح http://localhost:3000
```

## 🏗️ البناء للإنتاج

```bash
# بناء التطبيق
bun run build

# تشغيل خادم الإنتاج
bun run start
```

## 🐳 Docker

```bash
# بناء الصورة
docker build -t videohub-pro .

# تشغيل الحاوية
docker run -p 3000:3000 -v videohub-data:/app/data videohub-pro
```

## ⚙️ GitHub Actions Workflow

ملف `.github/workflows/build.yml` يقوم بـ:

1. **🔍 Quality Checks** — ESLint + TypeScript type check
2. **🏗️ Build** — بناء تطبيق الإنتاج + packaging كـ standalone server
3. **📦 Artifacts** — رفع `videohub-pro-standalone.tar.gz` و `.zip` كـ artifacts
4. **🐳 Docker Image** — بناء ورفع صورة Docker إلى GitHub Container Registry
5. **🚀 Release** — إنشاء GitHub Release تلقائي عند دفع tag (`v*`)

### تشغيل الـ Workflow

- **تلقائي**: عند push لـ `main`/`master` أو عند إنشاء PR
- **يدوي**: من تبويب Actions في GitHub → "Build & Release" → "Run workflow"
- **Release**: عند دفع tag مثل `git tag v1.0.0 && git push origin v1.0.0`

### تنزيل الـ Artifacts

بعد تشغيل الـ workflow بنجاح:
1. اذهب إلى تبويب **Actions** في GitHub
2. اختر آخر تشغيل
3. نزّل `videohub-pro-standalone-{build-number}` (tar.gz أو zip)
4. استخرج الملفات وشغّل `./start.sh`

## 📁 البنية

```
src/
├── app/
│   ├── api/videohub/       # REST API endpoints
│   │   ├── resolve/        # تحليل الروابط
│   │   ├── tasks/          # إدارة المهام
│   │   ├── plugins/        # إدارة الوحدات
│   │   ├── engine/tick/    # محرك التنزيل
│   │   ├── stats/          # الإحصائيات
│   │   ├── notifications/  # الإشعارات
│   │   ├── settings/       # الإعدادات
│   │   └── faults/         # تقارير الأعطال
│   ├── layout.tsx          # RTL + Arabic
│   └── page.tsx            # الصفحة الرئيسية
├── components/videohub/    # مكونات الواجهة
├── lib/videohub/           # الأنواع + الوحدات + الخدمات
├── store/videohub.ts       # متجر Zustand
└── prisma/schema.prisma    # قاعدة البيانات
```

## 🛠️ التقنيات

- **Frontend**: Next.js 16, TypeScript 5, Tailwind CSS 4, Framer Motion, Recharts
- **Backend**: Next.js API Routes, Prisma ORM, SQLite
- **State**: Zustand
- **Notifications**: Web Notifications API
- **Containerization**: Docker (multi-stage build)
