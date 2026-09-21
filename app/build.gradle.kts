import java.util.Properties

plugins {
    id("com.android.application")
}

/**
 * Sekrety podpisywania trzymamy poza skryptem budowania: najpierw local.properties,
 * potem zmienne środowiskowe. local.properties nie jest wersjonowany.
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String? = localProps.getProperty(key) ?: System.getenv(key)

val releaseStorePassword = secret("DROPME_STORE_PASSWORD")
val releaseKeyAlias = secret("DROPME_KEY_ALIAS")
val releaseKeyPassword = secret("DROPME_KEY_PASSWORD")

android {
    namespace = "com.example.testapp"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.dropme.app"
        minSdk = 24
        targetSdk = 36
        // Jedno źródło prawdy dla wersji (wcześniej dublowała się w AndroidManifest.xml).
        versionCode = 23
        versionName = "5.7"
    }

    signingConfigs {
        if (releaseStorePassword != null) {
            create("release") {
                storeFile = rootProject.file(secret("DROPME_STORE_FILE") ?: "release.keystore")
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // Build debug też podpisujemy kluczem release, jeśli jest dostępny.
            // Certyfikat "CN=Android Debug" jest wspólny dla wszystkich deweloperów,
            // więc Google Play Protect traktuje takie APK jako niezaufane i blokuje
            // instalację ("aplikacja może być niebezpieczna").
            if (releaseStorePassword != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            // R8: usuwanie nieużywanego kodu + obfuskacja (mniejszy i szybszy APK).
            isMinifyEnabled = true
            // Usuwanie zasobów, do których nie ma referencji.
            isShrinkResources = true
            if (releaseStorePassword != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Moduły funkcyjne wnoszą własne layouty + kontrolery.
    implementation(project(":feature-auth"))
    implementation(project(":feature-dashboard"))
    implementation(project(":feature-paywall"))
    implementation(project(":feature-profile"))

    // Warstwa wspólna i danych.
    implementation(project(":core"))
    implementation(project(":data"))

    // Uwaga: aplikacja nie używa żadnej biblioteki AndroidX ani Material.
    // Motyw to platformowy Theme.Material, a dialogi to android.app.AlertDialog.
    // Dlatego nie ma tu appcompat / material / constraintlayout ani core-ktx
    // (core-ktx wciągał dodatkowo kotlin-stdlib i kotlinx-coroutines do APK).
    testImplementation("junit:junit:4.13.2")
}

// Automatyczne kopiowanie świeżo skompilowanego APK do katalogu głównego projektu dla serwera WWW
tasks.register("copyApkToRoot") {
    description = "Kopiuje najświeższy plik APK do głównego katalogu projektu dla serwera WWW"
    doLast {
        val relSigned = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        val relUnsigned = layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk").get().asFile
        val releaseApk = if (relSigned.exists()) relSigned else if (relUnsigned.exists()) relUnsigned else null

        if (releaseApk != null && releaseApk.exists()) {
            val rootApk = rootProject.file("app-release.apk")
            releaseApk.copyTo(rootApk, overwrite = true)
            println("✓ [Auto-Copy] Zaktualizowano ${rootApk.name} w głównym katalogu (${releaseApk.length() / 1024} KB)")
        }
        val debugApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        if (debugApk.exists()) {
            val rootDebugApk = rootProject.file("app-debug.apk")
            debugApk.copyTo(rootDebugApk, overwrite = true)
            println("✓ [Auto-Copy] Zaktualizowano ${rootDebugApk.name} w głównym katalogu (${debugApk.length() / 1024} KB)")
        }
    }
}

tasks.matching { it.name in listOf("assembleRelease", "assembleDebug", "assemble") }.configureEach {
    finalizedBy("copyApkToRoot")
}
