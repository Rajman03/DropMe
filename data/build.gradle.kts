plugins {
    id("com.android.library")
}

android {
    namespace = "com.example.testapp.data"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // API publiczne :data wystawia typy z :core (AuthUser, UserProfile).
    api(project(":core"))
}
