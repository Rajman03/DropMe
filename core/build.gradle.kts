import java.util.Properties

plugins {
    id("com.android.library")
}

/**
 * Sekrety trzymamy poza repozytorium. Kolejnosc zrodel (pierwsze wygrywa):
 *   1. wlasciwosc Gradle (-PDROPME_TELEGRAPH_TOKEN=...)
 *   2. local.properties
 *   3. zmienna srodowiskowa
 *
 * Wlasciwosc Gradle jest pierwsza specjalnie po to, zeby publikowany APK dalo sie
 * zbudowac BEZ sekretu, nie ruszajac lokalnego local.properties:
 *
 *     ./gradlew assembleRelease -PDROPME_TELEGRAPH_TOKEN=
 *
 * Wczesniej token byl wpisany na sztywno w kodzie zrodlowym, wiec kazdy, kto mial
 * repozytorium albo APK, mogl publikowac w imieniu wlasciciela. Brak sekretu nie
 * psuje budowania - wylacza tylko funkcje publikacji karty online.
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String? =
    project.findProperty(key)?.toString()
        ?: localProps.getProperty(key)
        ?: System.getenv(key)

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
