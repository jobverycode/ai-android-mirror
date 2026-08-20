import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ai.mirror"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ai.mirror"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Network & Concurrency
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.java.websocket)
    implementation(libs.gson)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)

    // Android Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register("installAllDebug") {
    group = "install"
    description = "Installs the debug APK to all connected ADB devices."
    dependsOn("assembleDebug")
    doLast {
        val apkFile = file("build/outputs/apk/debug/app-debug.apk")
        if (!apkFile.exists()) {
            throw GradleException("APK not found at ${apkFile.absolutePath}")
        }
        val adb = android.adbExecutable.absolutePath
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine(adb, "devices")
            standardOutput = stdout
        }
        val lines: List<String> = stdout.toString().lines()
        val devices: List<String> = lines
            .drop(1)
            .map { line -> line.trim() }
            .filter { line -> line.endsWith("device") }
            .map { line -> line.substringBefore("\t").substringBefore(" ").trim() }
            .filter { line -> line.isNotBlank() }

        if (devices.isEmpty()) {
            println("⚠️ No ADB devices connected.")
        } else {
            println("📱 Found ${devices.size} connected device(s): $devices")
            for (serial in devices) {
                println("🚀 Installing APK to device: $serial...")
                val result = exec {
                    isIgnoreExitValue = true
                    commandLine(adb, "-s", serial, "install", "-r", apkFile.absolutePath)
                }
                if (result.exitValue == 0) {
                    println("✅ Successfully installed on $serial")
                } else {
                    println("❌ Failed to install on $serial (exit code: ${result.exitValue})")
                }
            }
        }
    }
}
