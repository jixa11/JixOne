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
        versionCode = 8
        versionName = "1.2.0-beta4"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../jixone.keystore")
            storePassword = "jixone2026"
            keyAlias = "jixone"
            keyPassword = "jixone2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
