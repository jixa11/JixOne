# JixOne

> A beautiful, ad-free music player for the YouTube Music catalog — a fully standalone Android app (web UI bundled inside the APK) plus a Next.js web app.
>
> پلیر موزیک زیبا و بدون تبلیغ برای کاتالوگ YouTube Music — اپ اندروید کاملاً مستقل (رابط وب داخل خود APK) + نسخه وب (Next.js).

---

## Screenshots

| Home | Now Playing |
| --- | --- |
| ![Home](docs/screenshots/home.png) | ![Player](docs/screenshots/player.png) |

| Settings | Mobile |
| --- | --- |
| ![Settings](docs/screenshots/settings.png) | ![Mobile](docs/screenshots/mobile.png) |

---

## English

### Features

- **Full YouTube Music catalog** — search songs and artists and start listening instantly.
- **Ad-free playback engine** — audio streams are extracted directly on your device (youtubei.js in the browser, same trusted approach as Metrolist). If extraction fails, the app automatically falls back to the official YouTube player.
- **Downloads & offline playback** — save songs to device storage (OPFS) at 160 / 70 / 50 kbps Opus, watch live progress in the download queue, and play them back with zero network.
- **Smart download queue** — sequential processing, retry on failure, automatic pause/resume when the network drops.
- **Library** — liked songs, listening history, and custom playlists (create, rename, delete, add/remove songs).
- **Queue panel** with shuffle and repeat (off / all / one), drag-free reordering, jump-to-track.
- **Video support** — switch between hidden / mini / theater video modes of the official player.
- **8 hand-crafted multi-hue themes** — Cyber Red (default), Cyber Blue, Synthwave, Spotify, OLED, Dubai, YT Light, YT Original. Each theme is a full palette (surfaces, borders, text tiers, accent + glow), not just two colors.
- **Persian & English** — automatic RTL/LTR layout flip, Vazirmatn typography, locale-aware digits.
- **Google sign-in** — see your existing YouTube Music playlists inside the app (YouTube Data API v3). Runs in demo mode when no client ID is configured; guests can use everything else without an account.
- **Professional animations** — staggered list entrances, player slide-in, panel transitions, theme cross-fade, hover micro-interactions, and full `prefers-reduced-motion` support.
- **Standalone Android APK** — the whole web UI ships inside the app: install it and it just works, with **no server and no setup** (optional custom-server mode for power users in Settings).
- **Android extras** — MediaStyle lock-screen notification with play/pause/next/previous and a live seekbar, plus a resizable 4×1 home-screen widget that follows the app's accent color.

### Tech stack

| Layer | Tools |
| --- | --- |
| Web | Next.js 16 (App Router), TypeScript, Tailwind CSS 4, shadcn/ui |
| State | Zustand (player / library / downloads / settings / view) |
| Extraction | youtubei.js (web bundle, runs client-side) |
| Offline storage | OPFS (Origin Private File System) |
| Android | Kotlin WebView shell, MediaSessionCompat, RemoteViews widget |

### Run the web app

```bash
npm install
npm run dev
# open http://localhost:3000
```

Production build:

```bash
npm run build && npm start
```

### Google sign-in (optional)

1. Create an OAuth 2.0 **Web** client ID in [Google Cloud Console](https://console.cloud.google.com/apis/credentials).
2. Add your deployment origin (e.g. `http://localhost:3000`) to **Authorized JavaScript origins**.
3. Put the client ID in `.env`:

```
NEXT_PUBLIC_GOOGLE_CLIENT_ID=your_client_id_here
```

Without a client ID the account section offers a demo sign-in so you can still try the playlist-import flow.

### Android app

The Android app is **fully standalone**: the entire web app is bundled inside the APK and served locally over a secure origin (`WebViewAssetLoader`), so search, playback, downloads and OPFS offline storage all run on the device. Install and play — no server URL is ever asked.

Rebuild everything (static web export → embed into assets → signed release APK) with one script:

```bash
./build-apk.sh
# result: download/JixOne-v1.1.0.apk
```

Requirements: Node.js, JDK 17+, Android SDK (platform 34 + build-tools 34), Gradle 8.7+.

- **Advanced:** Settings → *Custom server* lets a power user point the app at their own hosted JixOne web server instead of the bundled UI.
- Signing is pre-configured in `app/build.gradle.kts` and expects `android/jixone.keystore` (**not committed — keep a private backup**, it is required to sign future updates with the same identity).

### How playback works

The browser extracts audio streams directly from YouTube using its public web API, from **your own IP** — no middleman server, exactly like Metrolist. This means playback quality depends on your network: on datacenter/sandbox IPs YouTube may block extraction, while residential IPs and VPNs work normally.

### Disclaimer

JixOne is a client for publicly available YouTube content and hosts no media itself. Use it in accordance with YouTube's Terms of Service and your local copyright laws. This project is for personal, non-commercial use.

---

## فارسی

### امکانات

- **دسترسی کامل به کاتالوگ YouTube Music** — جستجوی آهنگ و خواننده و پخش فوری.
- **موتور پخش بدون تبلیغ** — استریم صوتی مستقیماً روی دستگاه خودت استخراج میشه (youtubei.js داخل مرورگر، همون روش مطمئن Metrolist). اگه استخراج ناموفق باشه، برنامه خودکار به پلیر رسمی یوتیوب سوئیچ می‌کنه.
- **دانلود و پخش آفلاین** — ذخیره آهنگ‌ها توی حافظه دستگاه (OPFS) با کیفیت ۱۶۰ / ۷۰ / ۵۰ کیلوبیت Opus، نمایش زنده پیشرفت توی صف دانلود، و پخش بدون اینترنت.
- **صف دانلود هوشمند** — پردازش ترتیبی، تلاش مجدد بعد از خطا، توقف/ادامه خودکار هنگام قطع اینترنت.
- **کتابخانه** — علاقه‌مندی‌ها، تاریخچه پخش و پلی‌لیست‌های شخصی (ساخت، تغییر نام، حذف، افزودن آهنگ).
- **صف پخش** با پخش تصادفی و تکرار (خاموش / همه / یکی).
- **پشتیبانی ویدیو** — حالت‌های مخفی / مینی / تئاتر برای ویدیوی رسمی.
- **۸ تم چندرنگ دست‌ساز** — Cyber Red (پیش‌فرض)، Cyber Blue، Synthwave، Spotify، OLED، Dubai، YT Light و YT Original. هر تم یک پالت کامل است (سطوح، بوردرها، لایه‌های متن، رنگ اکسنت و درخشش) — نه فقط دو رنگ.
- **فارسی و انگلیسی** — چرخش خودکار راست‌به‌چپ/چپ‌به‌راست، فونت وزیرمتن و اعداد هم‌ساز با زبان.
- **ورود با گوگل** — پلی‌لیست‌های قبلی‌ات در YouTube Music داخل برنامه نمایش داده میشن (YouTube Data API v3). بدون Client ID هم حالت دمو داره؛ مهمان‌ها بدون حساب از بقیه امکانات استفاده می‌کنن.
- **انیمیشن‌های حرفه‌ای** — ورود پله‌ای لیست‌ها، اسلاید پلیر، ترنزیشن پنل‌ها، کراس‌فید تم، میکرواینترکشن‌های hover و پشتیبانی کامل از `prefers-reduced-motion`.
- **APK اندروید کاملاً مستقل** — کل رابط وب داخل خود اپ جاسازی شده: نصب کن و استفاده کن، **بدون هیچ سرور و تنظیماتی** (حالت «سرور دلخواه» برای کاربران حرفه‌ای در تنظیمات هست).
- **امکانات اندروید** — نوتیفیکیشن قفل‌صفحه با دکمه‌های پخش/توقف/بعدی/قبلی و نوار پیشرفت زنده، به‌همراه ویجت ۴×۱ قابل تغییر اندازه که رنگ اکسنت برنامه رو دنبال می‌کنه.

### اجرای نسخه وب

```bash
npm install
npm run dev
# باز کن http://localhost:3000
```

بیلد نهایی:

```bash
npm run build && npm start
```

### ورود با گوگل (اختیاری)

1. توی [Google Cloud Console](https://console.cloud.google.com/apis/credentials) یک OAuth Client ID از نوع **Web** بساز.
2. آدرس سایتت (مثل `http://localhost:3000`) رو توی **Authorized JavaScript origins** اضافه کن.
3. شناسه رو توی فایل `.env` بذار:

```
NEXT_PUBLIC_GOOGLE_CLIENT_ID=شناسه_تو
```

اگه شناسه تنظیم نشده باشه، بخش حساب کاربری حالت دمو داره تا فرایند ورود پلی‌لیست‌ها رو امتحان کنی.

### نسخه اندروید

اپ اندروید **کاملاً مستقله**: کل اپ وب داخل APK جاسازی شده و از یک origin امن (`WebViewAssetLoader`) روی خود گوشی اجرا می‌شه — یعنی جستجو، پخش، دانلود و حافظه آفلاین OPFS همه روی دستگاه کاربرن. نصب کن و پخش کن — هیچ‌وقت آدرس سرور پرسیده نمی‌شه.

بیلد کامل (خروجی استاتیک وب ← جاسازی در assets ← APK امضاشده) با یک اسکریپت:

```bash
./build-apk.sh
# خروجی: download/JixOne-v1.1.0.apk
```

پیش‌نیازها: Node.js، JDK 17+، اندروید SDK (platform 34 + build-tools 34)، Gradle 8.7+.

- **پیشرفته:** تنظیمات ← *سرور اختصاصی* به کاربر حرفه‌ای اجازه می‌ده اپ را به سرور وب شخصی خودش وصل کنه.
- تنظیمات امضا توی `app/build.gradle.kts` از قبل انجام شده و به `android/jixone.keystore` نیاز داره (**توی ریپو نیست — حتماً بکاپ خصوصی نگه دار**، برای امضای آپدیت‌های بعدی با همون هویت الزامیه).

### نحوه پخش چطور کار می‌کنه؟

مرورگر خودت استریم صوتی رو مستقیم و با **IP خودت** از API عمومی یوتیوب استخراج می‌کنه — بدون سرور واسط، دقیقاً مثل Metrolist. برای همین روی IPهای دیتاسنتر ممکنه یوتیوب جلوی استخراج رو بگیره، ولی با اینترنت خانگی یا VPN همه‌چیز عادی کار می‌کنه.

### سلب مسئولیت

JixOne یک کلاینت برای محتوای عمومی یوتیوب است و خودش هیچ مدیایی را میزبانی نمی‌کند. لطفاً از آن مطابق قوانین یوتیوب و قوانین کپی‌رایت محل زندگی‌ات استفاده کن. این پروژه برای استفاده شخصی و غیرتجاری است.
