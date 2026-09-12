#!/usr/bin/env bash
# JixOne — full standalone APK build
# 1) static web export (Next.js)  2) copy into Android assets  3) signed release APK
# Usage: ./build-apk.sh [version]   (default 1.2.0-beta1)
# Requirements: node/npm, JDK 17+, Android SDK (ANDROID_HOME or local.properties), Gradle 8.7+
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
VERSION="${1:-1.2.0-beta1}"
WEB_OUT="$ROOT/web-export"
ASSETS_WEB="$ROOT/android/app/src/main/assets/web"
APK_STASH="$ROOT/.apk_stash"

echo "==> [1/4] Static web build (no server needed)"
(
  cd "$ROOT"
  # API routes are dynamic server handlers — set them aside for the export build
  if [ -d src/app/api ]; then mv src/app/api src/.api_stash; fi
  # public/*.apk (beta downloads) must NOT be embedded into the app assets
  if ls public/*.apk >/dev/null 2>&1; then mkdir -p "$APK_STASH" && mv public/*.apk "$APK_STASH"/; fi
  restore() {
    cd "$ROOT"
    [ -d src/.api_stash ] && mv src/.api_stash src/app/api || true
    [ -d "$APK_STASH" ] && mv "$APK_STASH"/*.apk public/ 2>/dev/null || true
  }
  trap restore EXIT
  STATIC_EXPORT=1 npx next build
)
[ -d "$ROOT/out" ] && rm -rf "$WEB_OUT" && mv "$ROOT/out" "$WEB_OUT"
echo "    web-export: $(du -sh "$WEB_OUT" | cut -f1)"

echo "==> [2/4] Copy web app into Android assets"
rm -rf "$ASSETS_WEB"
mkdir -p "$ASSETS_WEB"
cp -r "$WEB_OUT"/. "$ASSETS_WEB"/

echo "==> [3/4] Gradle assembleRelease"
(
  cd "$ROOT/android"
  GRADLE_BIN="${GRADLE_BIN:-gradle}"
  "$GRADLE_BIN" assembleRelease --no-daemon
)

echo "==> [4/4] Collect APK"
mkdir -p "$ROOT/download"
cp "$ROOT/android/app/build/outputs/apk/release/app-release.apk" "$ROOT/download/JixOne-v$VERSION.apk"
sha256sum "$ROOT/download/JixOne-v$VERSION.apk"
echo "DONE → download/JixOne-v$VERSION.apk"
