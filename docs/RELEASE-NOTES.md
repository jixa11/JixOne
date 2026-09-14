## 🎵 JixOne v1.3.0

نسخه‌ای که ایرادهای روی گوشی واقعی را برمی‌دارد: دکمه‌ی بک، پخش در پس‌زمینه، ورود به حساب، و آیکون.

### 🐛 رفع اشکال

- **دکمه‌ی بک گوشی کل برنامه را می‌بست.** برنامه یک صفحه‌ی واحد با پشته‌ی ناوبری خودش است، پس `WebView.canGoBack()` همیشه false بود و هر بار بک زده می‌شد برنامه بسته می‌شد. حالا بک اول صف پخش را می‌بندد، بعد صفحه‌ی پخش را، بعد یک صفحه به عقب می‌رود، و فقط وقتی چیزی برای برگشتن نمانده از برنامه خارج می‌شود.
- **با قفل شدن صفحه یا رفتن به پس‌زمینه موزیک قطع می‌شد.** `WebView.onPause()` کل صفحه و با آن المان صوت را معلق می‌کرد. حالا فقط وقتی چیزی پخش نمی‌شود معلق می‌شود، و سرویس پخش تا پایان آهنگ wake lock نگه می‌دارد تا دستگاه وسط کار به خواب نرود.
- **ورود با پیام «this browser or app may not be secure» رد می‌شد.** صفحه‌ی ورود خودش را دسکتاپ کروم جا می‌زد — دقیقاً همان ناهماهنگی‌ای که گوگل دنبالش می‌گردد. حالا دقیقاً مثل Metrolist کار می‌کند: user agent دست‌نخورده، آدرس ساده‌ی ServiceLogin، و خواندن نشست از خود صفحه‌ی واردشده.
- **دانلود بعد از ورود «موفق» هنوز ارور لزوم ورود می‌داد.** `visitorData` از مسیر ناشناس ساخته می‌شد و به حساب تعلق نداشت، پس یوتیوب درخواست را همچنان خارج‌از‌حساب می‌دید. حالا `visitorData`، `dataSyncId` و شماره‌ی حساب از `window.yt.config_` همان صفحه برداشته می‌شوند.
- **آیکون خوانا نبود.** حلقه‌ی تزئینی دور نشان، تمام سطح قابل‌دیدن لانچر را می‌گرفت. حالا طرح از لبه‌ی ماسک بیرون می‌زند تا خود نشان در ۴۸dp هم خوانده شود.

### ✨ تازه‌ها

- **ورود با کوکی دستی** — اگر گوگل صفحه‌ی ورود داخل برنامه را قبول نکرد، در مرورگر دسکتاپ وارد music.youtube.com شو و مقدار Cookie را در تنظیمات ← حساب کاربری بچسبان. همان راه فراری که InnerTune و Metrolist هم می‌دهند.
- **صفحه‌ی اصلی حالا اکسپلور است** به‌جای جستجو.

### 📥 نصب

اندروید ۸.۰ به بالا. اگر نسخه‌ی قبلی نصب است اول آن را حذف کن — کلید امضا عوض شده. از نسخه‌ی بعد به بعد به‌روزرسانی بدون حذف انجام می‌شود.

---

## 🎵 JixOne v1.3.0 (English)

### 🐛 Fixes

- **The back button closed the whole app.** The app is one page with its own navigation stack, so `WebView.canGoBack()` was always false and every press dropped out of the app. Back now closes the queue, then the now-playing sheet, then pops a screen, and only leaves when there is genuinely nothing left.
- **Playback stopped when the screen locked or the app went to the background.** `WebView.onPause()` suspends the DOM and with it the audio element. It now only suspends when nothing is playing, and the media service holds a wake lock for the duration of the track.
- **Sign-in was refused with "this browser or app may not be secure".** The login screen spoofed a desktop Chrome user agent — exactly the mismatch Google looks for. It now reproduces Metrolist's flow: untouched user agent, plain ServiceLogin URL, session read from the signed-in page.
- **Downloads still failed with a login error after a "successful" sign-in.** The `visitorData` was minted from the anonymous bootstrap and did not belong to the account, so InnerTube kept treating calls as logged out. `visitorData`, `dataSyncId` and the account index now come from `window.yt.config_` on that same page.
- **The launcher icon was unreadable.** The badge's decorative ring ate the visible area; the artwork now overscans past the mask so the mark reads at 48dp.

### ✨ New

- **Sign in with a pasted cookie** when Google refuses the in-app screen — Settings → Account.
- **Explore is the landing view** instead of Search.

### 📥 Install

Android 8.0+. Uninstall any previous JixOne first — the signing key changed. Later builds update in place.

### ⚠️ Note

JixOne hosts nothing; the catalogue comes from YouTube Music and streams are resolved on the device. A VPN is required where YouTube Music is unavailable.
