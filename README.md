# Wirtualna Wizytówka - Tworzenie & Usuwanie Kodów QR (v1.5)

Kompletna aplikacja na system **Android**, w której użytkownik ma **pełną swobodę tworzenia, edytowania i usuwania własnych kodów QR** dla dowolnych kont w mediach społecznościowych i linków.

---

## 📱 Najważniejsze funkcje w v1.5:

1. **➕ Samodzielne Tworzenie Kodów QR**:
   - Możliwość dodania dowolnego konta (np. *Mój Instagram*, *TikTok*, *Discord*, *Facebook*, *YouTube*, *Twitch*, *Portfolio* itp.).
   - Wybór dedykowanej ikony: 📸, 🎵, 💼, 🎮, 💬, 🎥, 🌐.
   - Wklejenie własnego adresu URL do profilu.
   - Natychmiastowe wygenerowanie kodu QR po zatwierdzeniu!

2. **🗑️ Usuwanie Kodów QR**:
   - Każda karta na liście posiada czerwony przycisk **„Usuń 🗑️”**.
   - Bezpieczne okno dialogowe potwierdzające usunięcie.
   - Natychmiastowe usunięcie kodu z listy oraz z pamięci telefonu (`SharedPreferences`).

3. **📱 Wyświetlanie i Skanowanie**:
   - Przycisk **„Pokaż Kod QR 📱”** otwiera duży, wyrazisty kod QR gotowy do natychmiastowego zeskanowania aparatem innego telefonu.
   - Opcja bezpośredniego otwarcia profilu (`ACTION_VIEW`) oraz kopiowania adresu do schowka.

4. **💎 System Rang i Paywall**:
   - **Guest (zwykły user)**: limit tworzenia do 2 kodów QR. Przy próbie utworzenia trzeciego pojawia się okno z propozycją przejścia do Paywalla.
   - **Premium**: **nielimitowane tworzenie kodów QR**, brak jakichkolwiek ograniczeń, złota odznaka.
   - **Owner (Creator) & Admin**: pełny dostęp root bez limitów.
   - **Symulator Rang** w zakładce Profilu pozwala natychmiast przetestować działanie aplikacji w każdej z 4 ról.

3. **💎 Paywall i System Rang**:
   - **Guest (zwykły user)**: darmowy dostęp do podstawowych 2 platform (Instagram, LinkedIn), pozostałe oznaczone są kłódką `🔒 Wymaga Premium`.
   - **Premium**: nielimitowane kody QR do wszystkich 8 social mediów, brak ograniczeń.
   - **Admin & Owner (Creator)**: pełny dostęp root i uprawnienia administratorskie.
   - **Symulator Rang** w zakładce Edycji pozwala jednym kliknięciem przetestować działanie każdego poziomu uprawnień!

---

## 📁 Struktura projektu

```text
/workspace/test/
├── app/
│   ├── build.gradle.kts           # Konfiguracja modułu aplikacji, zależności i SDK
│   ├── proguard-rules.pro         # Reguły optymalizacji i obfuskacji kodu
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Deklaracja aplikacji i aktywności głównej
│       │   ├── java/com/example/testapp/
│       │   │   ├── MainActivity.kt    # Główna aktywność i obsługa widoku
│       │   │   ├── CounterManager.kt  # Logika biznesowa licznika
│       │   │   └── Task.kt            # Model danych zadania
│       │   └── res/
│       │       ├── layout/
│       │       │   ├── activity_main.xml # Główny szablon ekranu
│       │       │   └── item_task.xml     # Szablon pojedynczego elementu listy
│       │       ├── values/
│       │       │   ├── colors.xml        # Paleta kolorów
│       │       │   ├── strings.xml       # Zasoby tekstowe
│       │       │   └── themes.xml        # Styl motywu Material 3
│       │       └── drawable/             # Ikony i wektory graficzne
│       └── test/
│           └── java/com/example/testapp/
│               └── ExampleUnitTest.kt    # Testy jednostkowe JUnit
├── gradle/wrapper/                # Gradle Wrapper (gradle-wrapper.jar & properties)
├── build.gradle.kts               # Główny skrypt budowania projektu
├── settings.gradle.kts            # Konfiguracja repozytoriów i modułów
├── gradle.properties              # Parametry środowiskowe JVM i AndroidX
└── gradlew                        # Skrypt wykonawczy Gradle
```

---

## 🚀 Jak uruchomić aplikację?

### Opcja 1: Błyskawiczna kompilacja na urządzeniu (Zalecane tutaj)

W projekcie przygotowany jest skrypt [`build.sh`](file:///workspace/test/build.sh), który kompiluje i podpisuje plik APK w kilkanaście sekund bez narzutu ciężkich wtyczek:

```bash
./build.sh
```

Wynikowy plik instalacyjny:
➡️ **[`/workspace/test/app-debug.apk`](file:///workspace/test/app-debug.apk)**

---

### Opcja 2: W Android Studio

1. Uruchom program **Android Studio**.
2. Wybierz **File** -> **Open...** i wskaż folder projektu: [`/workspace/test`](file:///workspace/test).
3. Android Studio zsynchronizuje projekt.
4. Kliknij zielony przycisk **Run** (`Shift + F10`).

### Opcja 3: Z wiersza poleceń (Gradle CLI)

Wymagane jest zainstalowane środowisko Java JDK (17+) oraz Android SDK:

* **Uruchomienie testów jednostkowych:**
  ```bash
  ./gradlew test
  ```

* **Zbudowanie pliku instalacyjnego APK (Debug):**
  ```bash
  ./gradlew assembleDebug
  ```
  Wygenerowany plik APK znajdziesz w:
  `app/build/outputs/apk/debug/app-debug.apk`

* **Instalacja bezpośrednio na podłączonym urządzeniu / emulatorze:**
  ```bash
  ./gradlew installDebug
  ```
