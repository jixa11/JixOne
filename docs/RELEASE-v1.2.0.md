## 🎵 JixOne v1.2.0

نسخه‌ای که ورود واقعی به یوتیوب موزیک، صفحه‌ی پخش تمام‌صفحه و نقش‌های ایرانی واقعی را می‌آورد.

### ✨ تازه‌ها

- 🔑 **ورود به یوتیوب موزیک** — با کوکی حساب (همان روش Metrolist). یوتیوب درخواست‌های ناشناس را bot-gate می‌کند و بدون ورود، پخش و دانلود روی بیشتر آهنگ‌ها کار نمی‌کند. تنظیمات ← حساب کاربری ← ورود.
- 🎨 **آیکون جدید برنامه** — آیکون قبلی placeholder بود.
- ▶️ **صفحه‌ی پخش تمام‌صفحه** — کاور بزرگ، شافل، ریپیت، صف پخش، لایک و دانلود. روی نوار پخش پایین بزن تا با انیمیشن بالا بیاید.
- 👆 **سوایپ روی ردیف آهنگ‌ها** — چپ: آهنگ بعدی، راست: افزودن به صف پخش.
- 🖼️ **۴ تم ایرانی با تصاویر واقعی** — فرش ایرانی، کاشی اصفهان، درفش کاویانی، کوروش هخامنشی.

### 🐛 رفع اشکال

- **دانلودها ناقص بودند** — یک آهنگ ۳.۴ مگابایتی فقط ۵۵٪ دانلود می‌شد. حالا فایل به‌صورت بازه‌ای خوانده می‌شود.
- **نوتیفیکیشن روی اندروید ۱۳ به بالا اصلاً نمایش داده نمی‌شد** — مجوز POST_NOTIFICATIONS هیچ‌وقت درخواست نمی‌شد.
- **سرویس پخش چند بار در ثانیه ری‌استارت می‌شد** — مصرف باتری و احتمال رد شدن توسط سیستم.
- زنجیره‌ی استخراج بازنویسی شد؛ کلاینت‌های ازکارافتاده حذف و visitorData اضافه شد.
- نقش پس‌زمینه روی متن می‌افتاد؛ موبایل دو نوار جستجو داشت؛ نوار پخش روی ستون کناری می‌افتاد.

### 📥 نصب

1. فایل APK را از پایین دانلود کنید (اندروید ۸.۰ به بالا)
2. اجازه‌ی «نصب از منابع ناشناس» را بدهید
3. ⚠️ اگر نسخه‌ی قبلی نصب است، **اول آن را حذف کنید** — این بیلد با کلید debug امضا شده و امضایش با نسخه‌های قبلی فرق دارد
4. برای اینکه پخش درست کار کند: تنظیمات ← حساب کاربری ← **ورود به یوتیوب موزیک**

### ⚠️ توضیح

JixOne هیچ محتوایی میزبانی نمی‌کند؛ کاتالوگ از YouTube Music می‌آید و استریم روی خود دستگاه استخراج می‌شود. اگر یوتیوب موزیک در منطقه‌ات در دسترس نیست، VPN لازم است.

---

## 🎵 JixOne v1.2.0 (English)

### ✨ New

- 🔑 **YouTube Music sign-in** via an account cookie, the way Metrolist does it. YouTube bot-gates anonymous requests, so without it playback and downloads fail on most tracks.
- 🎨 **Real app icon** — the previous one was a placeholder.
- ▶️ **Full-screen now-playing sheet** — large cover, shuffle, repeat, queue, like, download. Tap the player bar to raise it.
- 👆 **Swipe gestures on track rows** — left for the next track, right to add to the queue.
- 🖼️ **Four Persian themes with real artwork.**

### 🐛 Fixes

- **Downloads truncated at ~55%** — a 3.4 MB track arrived incomplete. Both the proxy and the OPFS writer now walk the file in byte ranges.
- **The media notification never appeared on Android 13+** — POST_NOTIFICATIONS was declared but never requested.
- **The playback service restarted several times a second** on position ticks.
- Extraction chain rebuilt on the clients that still answer, with visitorData.
- Background artwork painted over body text; mobile rendered two search bars; the player bar covered the sidebar.

### 📥 Install

Requires Android 8.0+. This build is debug-signed, so **uninstall any previous JixOne first** — the signature differs. Then sign in from Settings → Account.
