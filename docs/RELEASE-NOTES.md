## 🎵 JixOne v1.4.0

نسخه‌ی «وب‌اپ واقعی»: نصب‌پذیر، آفلاین‌پذیر، با CI درست‌شده.

### ✨ تازه‌ها

- **نصب به‌عنوان اپلیکیشن (PWA)** — نسخه‌ی وب جیکس‌وان حالا یک PWA کامل است. در تنظیمات ← «نصب به‌عنوان اپ» دکمه‌ی نصب را می‌بینی؛ برنامه آیکون اختصاصی می‌گیرد، مستقل و بدون نوار مرورگر باز می‌شود، پخش در پس‌زمینه ادامه دارد و کنترل پخش روی لاک‌اسکرین و نوار اعلان می‌آید. در iOS هم با Share ← Add to Home Screen نصب می‌شود.
- **پوسته‌ی آفلاین (Service Worker)** — صفحه‌ی اصلی و فایل‌های استاتیک کش می‌شوند؛ بعد از اولین بازدید، حتی بی‌اینترنت برنامه بالا می‌آید (آهنگ‌های دانلودشده همیشه آفلاین پخش می‌شدند — حالا خود برنامه هم آفلاین باز می‌شود). داده‌های زنده (`/api`، استریم‌ها و APK) هرگز کش نمی‌شوند.
- **کنترل‌های کامل لاک‌اسکرین در وب** — کنترل stop و جلو/عقب پرش ۱۰ثانیه‌ای به Media Session اضافه شد، و هر هندلر جداگانه try/catch می‌گیرد؛ قبلاً اگر مرورگر یکی از اکشن‌ها را پشتیبانی نمی‌کرد، بقیه‌ی هندلرها هم بی‌صدا از کار می‌افتادند.

### 🐛 رفع اشکال

- **ورک‌فلو رلیز CI از همان قدم نصب خراب بود** — `npm ci` اجرا می‌کرد در حالی که ریپو فقط `bun.lock` دارد و `package-lock.json` وجود ندارد؛ یعنی هیچ APK واقعاً از طریق GitHub Actions ساخته نمی‌شد. حالا با `bun install` نصب می‌شود و کار می‌کند.
- **لینک «دانلود APK» در تنظیمات روی نسخه‌ی قدیمی می‌ماند** — بعد از هر رلیز، APK جدید خودکار روی `public/JixOne-latest.apk` جایگزین و کامیت می‌شود؛ لینک همیشه آخرین نسخه را می‌دهد.
- **برچسب نسخه در سه جای برنامه سه رقم متفاوت و همگی قدیمی بود** (دکمه v1.2.0، برچسب v1.3.0، درباره‌ی برنامه v1.2.0) — حالا یک منبع واحد (`src/lib/appInfo.ts`) همه‌ی جاها را تغذیه می‌کند.
- **APK اندروید نسخه‌ی اشتباه معرفی می‌کرد** — `versionCode/versionName` در gradle هاردکد بود؛ بیلد ۱.۴.۰ هم روی گوشی «1.3.0 (10)» می‌ماند. حالا مستقیم از همان منبع واحد خوانده می‌شود.
- **بیلد standalone داخل workspace والد می‌شکست** — Next ریشه‌ی tracing را با بالا رفتن تا اولین lockfile تشخیص می‌دهد؛ ریپو را داخل هر workspace والد کلون کنی، `server.js` به مسیری تو در تو می‌رود و اسکریپت‌های build/start کار نمی‌کنند. حالا `outputFileTracingRoot` روی ریشه‌ی خود ریپو پین شده.

### 📥 نصب

اندروید ۸.۰ به بالا. اگر روی نسخه‌ی نصب‌شده‌ی قبلی کلید امضای متفاوتی دارد، اول آن را حذف کن (اگر secret کلید امضا در ریپو تنظیم نشده باشد، بیلد CI با کلید debug امضا می‌شود). نسخه‌ی وب را هم می‌توانی از همین مرورگر به‌عنوان اپ نصب کنی.

---

## 🎵 JixOne v1.4.0 (English)

### ✨ New

- **Install as an app (PWA)** — the web build is now a full PWA. Settings → "Install as an app" shows the install button; the app gets its own icon, launches standalone without browser chrome, keeps playing in the background and surfaces lock-screen / notification controls. On iOS: Share → Add to Home Screen.
- **Offline shell (service worker)** — the shell and static assets are cached; after the first visit the app even opens offline (downloaded songs were always offline-playable — now the app itself opens offline too). Live data (`/api`, streams, APKs) is never cached.
- **Complete lock-screen controls on the web** — added `stop` and ±10s seek to Media Session, and each handler gets its own try/catch: previously one unsupported action silently killed every handler after it.

### 🐛 Fixes

- **The release CI workflow was broken from its very first step** — it ran `npm ci`, but the repo ships `bun.lock` and no `package-lock.json`, so no APK was ever actually built through GitHub Actions. It now installs with `bun install` and works.
- **The in-app "Get the app" link kept serving a stale APK** — after each release the fresh APK is now copied over `public/JixOne-latest.apk` and committed automatically.
- **Three different stale version labels** (button v1.2.0, APK label v1.3.0, About v1.2.0) — now a single source (`src/lib/appInfo.ts`) feeds them all.
- **The Android APK introduced itself with the wrong version** — versionCode/versionName were hardcoded in gradle; a 1.4.0 build would still report "1.3.0 (10)" on the phone. They are now read from the same single source.
- **Standalone builds broke inside a parent workspace** — Next infers the tracing root by walking up to the first lockfile; clone the repo inside any parent workspace and `server.js` lands in a nested path that the build/start scripts never find. `outputFileTracingRoot` is now pinned to the repo root.

### 📥 Install

Android 8.0+. If the previously installed copy has a different signing key, uninstall it first (without the signing-key secret configured, CI builds are debug-signed). The web build can be installed as an app straight from the browser.

---

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
