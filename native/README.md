# JixOne (native)

The native Android build of JixOne — a fork of [Metrolist](https://github.com/metrolistgroup/metrolist), GPL-3.0.

This replaces the WebView build in `../android`, which could not do real
background playback, an account-bound InnerTube session, or reliable
downloads. All three are things a native Media3 player gets right by
construction.

See `NOTICE.md` for attribution and `../docs/SIGNING.md` for the signing key.

## Build

```bash
cd native
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleFossDebug     # or assembleFossRelease
```

Flavours come from upstream: `foss` (no Cast), `gms` (Cast), `izzy`.
