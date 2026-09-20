import java.util.Properties

plugins {
    id("com.android.library")
}

/**
 * Sekrety trzymamy poza repozytorium: najpierw local.properties, potem zmienne
 * środowiskowe. Wcześniej token Telegra.ph byl wpisany na sztywno w kodzie
 * zrodlowym, wiec kazdy, kto mial repozytorium albo APK, mogl publikowac
 * w imieniu wlasciciela. Teraz brak sekretu nie psuje budowania - po prostu
 * wylacza funkcje publikacji karty online.
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String? = localProps.getProperty(key) ?: System.getenv(key)

val telegraphToken: String = secret("DROPME_TELEGRAPH_TOKEN") ?: ""

android {
    namespace = "com.example.testapp.core"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 24

        buildConfigField("String", "TELEGRAPH_TOKEN", "\"$telegraphToken\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // ZXing - lokalny JAR (generowanie i odczyt kodów QR w QRCodeHelper).
    implementation(files("libs/core-3.5.3.jar"))

    testImplementation("junit:junit:4.13.2")
    // W testach JVM org.json z android.jar to atrapa (rzuca "Stub!"),
    // więc podmieniamy go na realną implementację.
    testImplementation("org.json:json:20240303")
}
