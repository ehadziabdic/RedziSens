plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.stressmonitor"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.stressmonitor"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            pickFirsts.add("lib/x86/libfbjni.so")
            pickFirsts.add("lib/armeabi-v7a/libfbjni.so")
            pickFirsts.add("lib/arm64-v8a/libfbjni.so")
            pickFirsts.add("lib/x86_64/libfbjni.so")
        }
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // PyTorch Mobile
    implementation("org.pytorch:pytorch_android_lite:2.1.0")
    implementation("org.pytorch:pytorch_android_torchvision_lite:1.13.1")

    // Android Health/Sensor Services
    implementation("androidx.health:health-services-client:1.0.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-database")
}