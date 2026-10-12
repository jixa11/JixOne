plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Single source of truth for the version: src/lib/appInfo.ts (the web app's
// label). Previously versionCode/versionName were hardcoded here and drifted —
// the v1.4.0 APK would still have introduced itself as "1.3.0 (10)".
val appInfoTs = File(rootDir.parentFile, "src/lib/appInfo.ts").readText()
val jixVersionName: String =
    Regex("APP_VERSION\\s*=\\s*'([0-9]+\\.[0-9]+\\.[0-9]+)'")
        .find(appInfoTs)?.groupValues?.get(1) ?: "1.4.0"
// 1.4.0 → 10400; always increases, never collides with the old hand-bumped 10
val jixVersionCode: Int = jixVersionName.split(".").let {
    (it.getOrNull(0)?.toIntOrNull() ?: 1) * 10000 +
    (it.getOrNull(1)?.toIntOrNull() ?: 0) * 100 +
    (it.getOrNull(2)?.toIntOrNull() ?: 0)
}

android {
    namespace = "com.jixone.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jixone.app"
        minSdk = 26
        targetSdk = 34
        versionCode = jixVersionCode
        versionName = jixVersionName
    }

    // The release key is deliberately NOT in the repo — a committed signing key
    // lets anyone build an APK that Android accepts as a JixOne update. Create
    // one once and keep it (see docs/SIGNING.md); every build then installs
    // over the last without an uninstall. Without it the build still works and
    // falls back to the debug key, but each such build has a different
    // signature and has to replace the previous install.
    val releaseKeystore = file("../jixone.keystore")

    signingConfigs {
        if (releaseKeystore.exists()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = providers.gradleProperty("jixoneStorePassword").orNull
                    ?: System.getenv("JIXONE_STORE_PASSWORD")
                keyAlias = providers.gradleProperty("jixoneKeyAlias").orNull
                    ?: System.getenv("JIXONE_KEY_ALIAS") ?: "jixone"
                keyPassword = providers.gradleProperty("jixoneKeyPassword").orNull
                    ?: System.getenv("JIXONE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    androidResources {
        // default pattern ignores "<dir>_*" → would strip the "_next/" asset folder
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:!CVS:!thumbs.db:!picasa.ini:!*~"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.media:media:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
