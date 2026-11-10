# JixOne (native)

The native Android build of JixOne — a fork of [Metrolist](https://github.com/metrolistgroup/metrolist), GPL-3.0.

This replaces the WebView build in `../android`, which could not do real
background playback, an account-bound InnerTube session, or reliable
downloads. All three are things a native Media3 player gets right by
construction.

See `NOTICE.md` for attribution and `../docs/SIGNING.md` for the signing key.

## What JixOne adds on top of upstream

- JixOne identity: name, neon J-Note icon, `com.jixone.native` package id
- Persian theme palettes in Settings → Appearance → Theme:
  کاشی اصفهان (turquoise), فرش ایرانی (carpet gold), درفش کاویانی (violet), کوروش (blue)
- Brand default theme color matched to the app icon
- **Copy playlist**: duplicate any playlist with all songs and their order
- In-app updater pointed at this repository: releases tagged `n*`
  (e.g. `n2.0.2`) with `JixOne-<version>-<abi>.apk` + `JixOne-<version>-universal.apk` assets
- Farsi UI (upstream translations), first-run RTL experience

## Build

```bash
cd native
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleGmsDebug      # or assembleGmsRelease
```

Flavours come from upstream: `foss` (no Cast), `gms` (Cast), `izzy`.
The CI workflow (`.github/workflows/native-release.yml` at the repo root)
builds the `gms` flavour; pushing a tag like `n2.0.0` attaches the APK to a
GitHub release.

## Releases

Tags starting with `n` (native) build the Android app: `n2.0.0` → release
"JixOne 2.0.0 (native)" with `JixOne-2.0.0.apk` attached. The legacy `v*`
tags belong to the WebView-era builds and are ignored by the in-app updater.
