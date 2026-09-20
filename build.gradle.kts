// Główny plik budowania - wersje wtyczek wspólne dla wszystkich modułów.
//
// Projekt nie zawiera kodu Kotlin, dlatego wtyczka kotlin.android nie jest tu
// deklarowana - to usuwa kotlin-stdlib z APK i skraca czas budowania.
plugins {
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library") version "8.3.2" apply false
}
