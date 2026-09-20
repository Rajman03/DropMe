pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DropMe"

// --- Moduły wspólne ---
include(":core")     // modele, preferencje, platformy, QR, bezpieczeństwo, wspólne zasoby UI
include(":data")     // Supabase + AuthManager

// --- Moduły funkcyjne (każdy: własny layout + własny kontroler) ---
include(":feature-auth")
include(":feature-dashboard")
include(":feature-paywall")
include(":feature-profile")

// --- Aplikacja ---
include(":app")
