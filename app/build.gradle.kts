plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.rkdesign.glasslauncher"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.rkdesign.glasslauncher"
        minSdk = 26
        targetSdk = 28
        versionCode = 11
        versionName = "1.1.1-safe"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false } }
}
