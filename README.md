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
- 🚀 **وضعان للنشر** — Server (كامل الميزات) أو Static (GitHub Pages)

## 🏗️ وضعا النشر

يدعم التطبيق وضعين مختلفين:

### 1. Server Mode (الافتراضي)
- Next.js كامل مع API routes + Prisma/SQLite
- خدمة yt-dlp منفصلة (Python/FastAPI) للاستخراج والتنزيل
- التنزيلات تُحفظ على خادم Next.js
- مناسب للاستضافة الذاتية (VPS, Docker, إلخ)

### 2. Static Mode (GitHub Pages)
- Static export — كل المنطق في المتصفح
- المهام والإعدادات في localStorage
- التنزيلات تُحفظ مباشرة في مجلد التنزيلات لدى المستخدم عبر المتصفح
- يتطلب خدمة yt-dlp منشورة على استضافة (Render, Railway, Fly.io) أو محلية

## 🚀 التشغيل المحلي (Server Mode)

```bash
# تثبيت الاعتماديات
bun install

# تثبيت اعتماديات خدمة yt-dlp
pip3 install -r mini-services/yt-dlp-service/requirements.txt

# تهيئة قاعدة البيانات
bun run db:push

# تشغيل خدمة yt-dlp (في terminal منفصل)
bun run resolver:bg
# أو: cd mini-services/yt-dlp-service && python3 main.py

# تشغيل خادم التطوير
bun run dev

# فتح http://localhost:3000
```

## 🌐 النشر على GitHub Pages (Static Mode)

### الخطوة 1: أنشئ مستودعاً على GitHub وادفع الكود

```bash
git init
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin main
```

### الخطوة 2: فعّل GitHub Pages

1. اذهب إلى **Settings → Pages** في المستودع
2. تحت **Source**, اختر **GitHub Actions**
3. عند كل push على `main`, سيتم بناء الموقع تلقائياً ونشره على:
   - `https://<user>.github.io/<repo>/` (project pages)
   - `https://<user>.github.io/` (user/org pages — يجب تسمية المستودع `<user>.github.io`)

### الخطوة 3: شغّل خدمة yt-dlp (محلياً بدون خدمات مدفوعة)

المتصفح لا يمكنه استخراج روابط التنزيل من YouTube/TikTok مباشرة — يحتاج خدمة yt-dlp. لديك **4 خيارات** لتشغيلها محلياً بدون أي تكلفة:

---

#### 🟢 الخيار 1: على الكمبيوتر (الأسهل —推荐)

شغّل الخدمة على الكمبيوتر، والهاتف يتصل بها عبر شبكة Wi-Fi نفسها.

```bash
# 1. ثبّت Python 3.10+ من https://python.org (إذا لم يكن مثبتاً)
# 2. ثبّت ffmpeg:
#    Mac:    brew install ffmpeg
#    Linux:  sudo apt install ffmpeg
#    Windows: choco install ffmpeg

# 3. شغّل السكريبت (يكشف IP تلقائياً ويبدأ الخدمة)
bash scripts/start-local.sh
```

السكريبت سيعرض:
- ✅ حالة الخدمة (Python, ffmpeg, yt-dlp)
- 📌 `http://localhost:8001` للاستخدام على نفس الكمبيوتر
- 📱 `http://192.168.x.x:8001` للاستخدام من الهاتف على نفس Wi-Fi

ثم في التطبيق (على الهاتف أو الكمبيوتر):
1. افتح الإعدادات (⚙️)
2. أدخل الرابط الذي عرضه السكريبت
3. اضغط "اختبار الاتصال"

**ملاحظة:** على Linux قد تحتاج `sudo ufw allow 8001/tcp` للسماح بالاتصالات.

---

#### 🟢 الخيار 2: على الهاتف مباشرة (Termux — بدون كمبيوتر)

شغّل yt-dlp على هاتف أندرويد بدون root، باستخدام Termux.

```bash
# 1. نزّل Termux من F-Droid (لا تستخدم متجر Play — نسخته قديمة)
#    https://f-droid.org/packages/com.termux/

# 2. افتح Termux واكتب:
pkg install git
git clone https://github.com/<your-username>/<your-repo>.git ~/videohub
cd ~/videohub

# 3. شغّل السكريبت (يثبت Python + ffmpeg + yt-dlp تلقائياً)
bash scripts/start-termux.sh
```

السكريبت سيشغل الخدمة على `http://localhost:8001`. ثم:
1. افتح متصفح الهاتف (Chrome)
2. اذهب لتطبيقك على GitHub Pages
3. في الإعدادات، أدخل `http://localhost:8001`
4. ابدأ التنزيل! 🎉

**ملاحظة:** لا تغلق Termux وإلا ستتوقف الخدمة. استخدم `termux-wake-lock` لمنع النظام من إغلاقها.

---

#### 🟡 الخيار 3: Render مجاني (للاستخدام من أي مكان)

إذا أردت وصولاً من أي مكان (ليس فقط شبكة Wi-Fi نفسها):

1. اذهب إلى [render.com](https://render.com) وأنشئ حساب مجاني
2. **New + → Blueprint**
3. اختر مستودع GitHub — Render سيقرأ `render.yaml` تلقائياً
4. اضغط **Apply** — انتظر ~5 دقائق
5. انسخ الرابط (مثل `https://videohub-resolver.onrender.com`)
6. أدخل الرابط في إعدادات التطبيق

**حدود الخطة المجانية:**
- 750 ساعة/شهر (تكفي للاستخدام الشخصي)
- الخدمة تنام بعد 15 دقيقة من عدم النشاط
- 512MB RAM (كافية لـ yt-dlp، لكن 4K قد يفشل)

---

#### 🔵 الخيار 4: Docker (للمستخدمين المتقدمين)

```bash
# بناء الصورة
docker build -t videohub-resolver ./mini-services/yt-dlp-service

# تشغيلها
docker run -d -p 8001:8001 --name videohub-resolver videohub-resolver

# الخدمة تعمل على http://localhost:8001
```

---

### الخطوة 4: استخدم التطبيق

1. افتح رابط GitHub Pages
2. اذهب إلى **الإعدادات** وأدخل رابط خدمة yt-dlp
3. اضغط **اختبار الاتصال** — يجب أن يظهر ✓ متصل
4. ارجع للرئيسية والصق رابط فيديو (YouTube, TikTok, إلخ)
5. اختر الجودة واضغط **تنزيل**
6. سيُحفظ الملف في مجلد التنزيلات لديك

## 📋 المتغيرات البيئية

| المتغير | الوصف | الافتراضي |
|---------|-------|-----------|
| `NEXT_PUBLIC_DEPLOY_MODE` | `server` أو `static` | `server` |
| `NEXT_PUBLIC_BASE_PATH` | مسار قاعدة لمشروع GitHub Pages (مثل `/repo-name`) | فارغ |
| `NEXT_PUBLIC_RESOLVER_URL` | رابط خدمة yt-dlp | `http://localhost:8001` |
| `DATABASE_URL` | قاعدة بيانات SQLite (server mode فقط) | `file:./db/custom.db` |
| `VIDEOHUB_DOWNLOAD_DIR` | مجلد التنزيلات (server mode فقط) | `/home/z/my-project/download/videohub` |

انسخ `.env.example` إلى `.env` وعدّل القيم حسب احتياجك.

## 📁 هيكل المشروع

```
.
├── src/                              # كود Next.js (TypeScript/React)
│   ├── app/                          # App Router pages + API routes
│   ├── components/videohub/          # مكونات UI
│   ├── lib/videohub/
│   │   ├── backend.ts                # server-mode backend (Prisma)
│   │   ├── resolver.ts               # static-mode client (yt-dlp direct)
│   │   ├── storage.ts                # static-mode storage (localStorage)
│   │   ├── mode.ts                   # كشف وضع النشر
│   │   └── types.ts                  # أنواع البيانات
│   └── store/videohub.ts             # Zustand store (يدعم الوضعين)
├── mini-services/
│   └── yt-dlp-service/               # خدمة Python/FastAPI لاستخراج الوسائط
├── android/                          # تطبيق Android (Kotlin/Jetpack Compose)
├── prisma/schema.prisma              # مخطط قاعدة البيانات (server mode)
├── .github/workflows/deploy.yml      # GitHub Actions workflow للنشر
├── scripts/
│   ├── build-static.sh               # بناء static export
│   └── test-e2e-downloads.py         # اختبار تنزيلات حقيقية
└── next.config.ts                    # إعدادات Next.js (server/static)
```

## 🧪 الاختبار

```bash
# اختبار TypeScript
bunx tsc --noEmit

# اختبار تنزيلات حقيقية (يتطلب تشغيل yt-dlp service)
python3 scripts/test-e2e-downloads.py

# بناء static محلياً
NEXT_PUBLIC_DEPLOY_MODE=static bun run build:static
# الناتج في ./out/
```

## 🔧 استكشاف الأخطاء

### التنزيلات لا تعمل
1. تأكد أن خدمة yt-dlp تعمل: `curl http://localhost:8001/health`
2. في الـ UI, تحقق من مؤشر حالة الخدمة في أعلى الصفحة الرئيسية
3. اذهب للإعدادات وتأكد من صحة رابط الخدمة

### YouTube يطلب "Sign in to confirm you're not a bot"
- YouTube يفرض هذا على بعض الفيديوهات — قيد من YouTube نفسه
- الحل: استخدم فيديو آخر، أو أضف cookies للمتصفح في خدمة yt-dlp

### فشل البناء على GitHub Actions
- تأكد أن `NEXT_PUBLIC_DEPLOY_MODE=static` مضبوط في workflow
- تأكد أن `NEXT_PUBLIC_BASE_PATH` مطابق لاسم المستودع (للمشروع pages)

## 📜 الترخيص

MIT License — استخدمه بحرية لمشاريعك الشخصية والتجارية.


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
