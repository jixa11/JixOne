plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jixone.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jixone.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 10
        versionName = "1.3.0"
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
