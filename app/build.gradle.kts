import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("dev.detekt") version "2.0.0-alpha.6"
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Kredensial signing TIDAK disimpan di dalam file ini, melainkan di
// keystore.properties yang sudah masuk .gitignore.
//
// File kredensial hanya dibaca jika ada. Ini disengaja: bila signing config
// diwajibkan, setiap orang yang meng-clone repository ini akan gagal build
// karena tidak punya keystore. Dengan begini, clone baru tetap bisa dibangun
// (hasilnya APK unsigned), hanya pemilik keystore yang bisa membuat build
// tertandatangani.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.blescanner"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.blescanner"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Hanya didaftarkan bila file kredensial tersedia. Lihat catatan di atas.
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // null bila keystore.properties tidak ada, dan itu tidak-error:
            // Gradle tetap membangun APK, hanya tanpa tanda tangan.
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("androidx.fragment:fragment-ktx:1.9.1")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

}