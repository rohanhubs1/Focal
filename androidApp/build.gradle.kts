import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
android {
    namespace = "com.example.myapplication"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rohan.focalapplication"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "mode"
    productFlavors {
        create("demo") {
            dimension = "mode"
            applicationIdSuffix = ".demo"
            buildConfigField("Boolean", "IS_DEMO_MODE", "true")
        }
        create("full") {
            dimension = "mode"
            buildConfigField("Boolean", "IS_DEMO_MODE", "false")
        }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    // CameraX and Google ML Kit Barcode Scanning
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    // BouncyCastle for NXP secp128r1 curve originality verification
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    // AndroidX Core SplashScreen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Firebase dependencies strictly for FULL flavor (BoM without -ktx)
    "fullImplementation"(platform("com.google.firebase:firebase-bom:33.7.0"))
    "fullImplementation"("com.google.firebase:firebase-auth")
    "fullImplementation"("com.google.firebase:firebase-firestore")
    "fullImplementation"("com.google.firebase:firebase-functions")
    "fullImplementation"("com.google.firebase:firebase-messaging")
    "fullImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}

// Apply Google Services plugin for Full flavor
if (file("src/full/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Disable Google Services tasks for Demo variants so demo never processes or expects google-services.json
tasks.matching { it.name.contains("Demo", ignoreCase = true) && it.name.contains("GoogleServices", ignoreCase = true) }.configureEach {
    enabled = false
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}