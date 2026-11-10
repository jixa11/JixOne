# Signing JixOne builds

Android identifies an app by its signing key. Two APKs signed with different
keys are different apps as far as the phone is concerned: it refuses to install
one over the other and asks you to uninstall first, which wipes downloads and
the signed-in session.

So keep one key and reuse it for every release.

## Make the key once

```bash
cd android
keytool -genkeypair -v \
  -keystore jixone.keystore \
  -alias jixone -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=JixOne, OU=JixOne, O=JixOne, L=, ST=, C="
```

`keytool` asks for a password twice. Back up `android/jixone.keystore` and the
password somewhere safe — losing either means every future build needs an
uninstall, permanently.

## Keep it out of the repository

`.gitignore` already excludes `*.keystore`, and it should stay that way.
A key committed to a public repository is a key anyone can use to build an APK
that phones accept as a JixOne update.

## Build with it

```bash
JIXONE_STORE_PASSWORD=… JIXONE_KEY_PASSWORD=… ./build-apk.sh 1.3.0
```

or put the same values in `~/.gradle/gradle.properties`:

```properties
jixoneStorePassword=…
jixoneKeyAlias=jixone
jixoneKeyPassword=…
```

Building in GitHub Actions: base64 the keystore into a repository secret,
write it back to `android/jixone.keystore` in the workflow, and pass the
passwords as secrets too.

## Without a key

The build still works — it falls back to the debug key — but each such build
gets a fresh signature, so it cannot install over the previous one.
