# JixOne Native (Android)

> نسخهٔ نیتیو اپ JixOne — بر پایهٔ **OuterTune** (کاتالوگ YouTube Music)، با تم‌های ایرانی اختصاصی، کپی پلی‌لیست و آیکون جدید JixOne.
> The native Android edition of JixOne — built on the **OuterTune** codebase (GPL-3.0, see `LICENSE`), carrying JixOne's signature Persian themes, playlist copy and new icon.

**Package:** `com.jixone.app` · **Version:** 1.6.0 (installs over the previous JixOne app; debug builds use `com.jixone.app.debug` and live side-by-side)

---

## What's inside (changes over upstream OuterTune)

| Feature | Where |
| --- | --- |
| 4 signature Persian themes — فرش ایرانی، کاشی اصفهان، درفش کاویانی، کوروش هخامنشی | `app/src/main/java/com/dd3boh/outertune/ui/theme/JixOneThemes.kt` + picker in **Settings → Appearance → JixOne theme** (فارسی + English) |
| **Copy playlist** — کپی کامل پلی‌لیست با حفظ ترتیب آهنگ‌ها | playlist menu (long-press a playlist) → *Copy playlist* |
| New minimal launcher icon — نت با سر لوزی کاشی روی سرمه‌ای، تیرک فیروزه‌ای | `app/src/main/res/drawable/launcher_foreground.xml` (+ monochrome / themed icons) |
| Rebranding — JixOne name, `com.jixone.app`, APK name `JixOne-1.6.0-…` | `app/build.gradle.kts`, `res/values/app_name.xml` |

Upstream: [OuterTune](https://github.com/OuterTune/OuterTune) — all upstream features are included: play/pause/next/previous/shuffle/repeat, queue management, playlist create/edit/delete, song & playlist downloads (offline), liked songs + account sync with YouTube Music, lyrics, sleep timer, M3U import/export, full Persian UI (`values-fa`), and more.

## Build

```bash
cd native
./gradlew assembleCoreDebug        # → app/build/outputs/apk/core/debug/JixOne-…apk (installable)
./gradlew assembleCoreRelease      # unsigned release (add your keystore via keystore.properties)
```

Or just push a tag `n*` (e.g. `n1.6.0`) — GitHub Actions builds the APK (`.github/workflows/native-release.yml`).

Requires JDK 21 + Android SDK (compileSdk 37).
