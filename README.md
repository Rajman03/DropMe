# DropMe — wizytówka QR

**Wszystkie Twoje linki w jednym kodzie QR.**

> Najnowsza zmiana (v5.7): uproszczenie panelu zarządzania użytkownikami — zmiana
> PIN-u odbywa się wyłącznie w Profilu użytkownika (panel nie pozwala już zmieniać
> PIN-u Twórcy, adminów ani własnego). Poprawki w Paywall: blokada podwójnych
> zakupów (debounce), naprawiony tekst „dodatkowej kawy" po zakupie Premium oraz
> jasny komunikat przy kolejnym wsparciu.

DropMe to lekka aplikacja na Androida, która zamienia profile w mediach społecznościowych
w jeden zeskanowalny kod QR. Tworzysz wizytówkę, dodajesz konta i linki, a druga osoba
skanuje kod i od razu trafia na właściwe miejsce — bez przepisywania nicków.

Aplikacja działa bez zewnętrznych bibliotek UI: motyw jest platformowy
(`Theme.Material`), dialogi to natywne `AlertDialog`, a kody QR generuje ZXing.
Efektem jest APK poniżej 200 kB.

---

## ⬇️ Pobierz

| | |
|---|---|
| **Plik** | [`app-release.apk`](app-release.apk) |
| **Wersja** | 5.7 (versionCode 23) |
| **Rozmiar** | ~199 kB |
| **Android** | 7.0 (API 24) i nowszy |
| **Podpis** | klucz release (`CN=DropMe Release`), schematy v1 + v2 + v3 |

Plik `app-release.apk` leży w głównym katalogu repozytorium — kliknij go na GitHubie,
a następnie **Download**. Link do bezpośredniego pobrania ma postać:

```text
https://github.com/<twoj-login>/<repo>/raw/main/app-release.apk
```

### Instalacja

1. Skopiuj APK na telefon (albo pobierz go bezpośrednio na urządzeniu).
2. Otwórz plik i zezwól na instalację z nieznanych źródeł, jeśli system o to poprosi.

#### ⚠️ Konflikt podpisu („Aplikacja nie została zainstalowana")

Wersja **5.6 z 21.09 (poprzedni wieczór)** została podpisana innym kluczem niż
pozostałe wydania. Android nie pozwala nadpisać aplikacji innym podpisem, więc
jeśli masz dokładnie tę wersję, aktualizacja zawsze się nie powiedzie. Rozwiązanie
jednorazowe:

1. **Odinstaluj** DropMe z telefonu (dane profilu wrócą po zalogowaniu — są w Supabase,
   a kody QR zapisane w chmurze się zsynchronizują).
2. Zainstaluj najnowszy `app-release.apk` (5.7 lub nowszy).
3. Kolejne aktualizacje pójdą już normalnie „na wierzch” — podpis jest od teraz spójny.

#### 🛡️ Ekran Google Play Protect przy instalacji

Aplikacja jest dystrybuowana poza Google Play, więc Play Protect może pokazać ekran
z logo Google („Aplikacja niezweryfikowana” / „niebezpieczna”). To normalne dla
sideloadu:

- dotknij **Szczegóły** / „Więcej”, a następnie **Zainstaluj mimo to**,
- albo na czas instalacji wyłącz skanowanie: Sklep Play → **Play Protect** →
  koło zębate → wyłącz **Skanowanie aplikacji w Play Protect**.

> Nie da się tego „naprawić” w kodzie aplikacji — to zabezpieczenie Androida dla
> aplikacji spoza Sklepu.

<details>
<summary>Co dokładnie znajduje się w tym APK</summary>

Ten build jest celowo zbudowany **bez prywatnych tokenów** — publikacja karty online
wymaga Twojego własnego klucza (patrz sekcja o budowaniu). W pliku nie ma żadnego
sekretu poza publicznym kluczem API Supabase, który z natury jest częścią aplikacji
klienckiej.

| | |
|---|---|
| versionCode / versionName | 23 / 5.7 |
| minSdk / targetSdk | 24 / 36 |
| Zasoby | 333 |
| Rozmiar `classes.dex` | ~148 kB |
| Minifikacja | R8 + `shrinkResources` |
| Zależności UI | brak |

</details>

---

## ✨ Funkcje

### Kody QR
- **11 typów profili**: Instagram, TikTok, Snapchat, Facebook, YouTube, LinkedIn,
  X (Twitter), Discord, GitHub, Spotify oraz dowolna strona WWW.
- Wklejasz **nick albo pełny link** — aplikacja sama rozpoznaje platformę
  i buduje poprawny adres profilu.
- Wybór **koloru kodu QR** i **ikony** kafelka.
- **Skalowanie kodu** do rozmiaru ekranu — gotowy do zeskanowania z drugiego telefonu.
- **Skaner wbudowany w aplikację** — odczytuje kody QR innych osób bez wychodzenia z DropMe.

### Wizytówka
- **Wielolinkowy profil** — jedna karta zbiera wszystkie Twoje konta i linki.
- **Eksport vCard** — zapis do kontaktu w telefonie (`.vcf`, zgodny z Instagramem,
  TikTokiem, Twitterem i LinkedIn przez `X-SOCIALPROFILE`).
- **Karta online** — publikacja wizytówki jako strony otwieranej w każdej przeglądarce,
  razem z kodem QR prowadzącym do niej.
- **Udostępnianie** kodu i linku przez standardowe menu Androida.

### Konto i chmura
- **Rejestracja i logowanie** z walidacją loginu i hasła.
- **Synchronizacja z Supabase** — ranga, avatar, nazwa i status Premium trzymają się
  między urządzeniami; aplikacja cache'uje dane lokalnie i działa też offline.
- **Ochrona przed zgadywaniem hasła** — po 5 nieudanych próbach kolejne są blokowane,
  z opóźnieniem rosnącym do 15 minut.
- **PIN zabezpieczający** dla akcji administracyjnych (4 cyfry, blokowane są trywialne
  kombinacje typu `1234`).
- **System rang**: Gość → Premium → Admin → Właściciel (Creator), z panelem zarządzania
  użytkownikami: nadawanie i odbieranie Premium, zmiana rangi, usuwanie konta.
- **Wsparcie przez PayPal** — dobrowolna „kawa" dla twórcy, z wyborem kwoty.

### Prywatność
- Polityka prywatności dostępna z wnętrza aplikacji.
- Hasła i PIN-y są **solone i hashowane** (PBKDF2-HMAC-SHA256, 20 000 iteracji)
  i nigdy nie są zapisywane w postaci jawnej.

---

## 🏗️ Architektura

Projekt jest podzielony na moduły, więc każda zmiana dotyka tylko jednego obszaru —
dodanie platformy, ekranu czy reguły uprawnień nie wymaga czytania całego kodu.

```text
DropMe/
├── core/                 # fundament bez UI: modele, preferencje, QR, bezpieczeństwo
│   ├── SocialPlatform     # 11 platform + rozpoznawanie linków i budowa adresów
│   ├── QRCodeHelper       # generowanie i skalowanie kodów QR (ZXing)
│   ├── MultiLinkProfile   # wizytówka: vCard, karta online
│   ├── CredentialHasher   # PBKDF2 + wykrywanie słabych PIN-ów
│   ├── PinSecurityManager # dialogi i weryfikacja PIN-u
│   ├── LoginThrottle      # blokada po nieudanych próbach logowania
│   └── UserProfile        # lokalny stan profilu i ranga
│
├── data/                 # warstwa danych — jedyne miejsce, które zna sieć
│   ├── SupabaseClient     # REST: użytkownicy, rangi, zakupy
│   ├── SupabaseConfig     # adres projektu i klucz API
│   └── AuthManager        # sesja, rejestracja, logowanie, synchronizacja
│
├── feature-auth/         # ekran rejestracji i logowania
├── feature-dashboard/    # lista kodów QR, formularz dodawania, skaner
├── feature-paywall/      # ekran wsparcia i wybór kwoty
├── feature-profile/      # profil użytkownika, panel administracyjny
│
└── app/                  # MainActivity, nawigacja, motyw, scalanie ekranów
```

**Zasada zależności:** `feature-*` → `data` → `core`. Moduły funkcyjne nie znają się
wzajemnie, a `:app` tylko je składa.

### Interfejs

Główny ekran to jeden `activity_main.xml` złożony z include'ów per zakładka
(`layout_dashboard`, `layout_paywall`, `layout_profile`, `layout_auth`), więc
dotknięcie jednego widoku nie wymaga przewijania tysięcy linii XML.

### Stos technologiczny

| Element | Wersja / wybór |
|---|---|
| Język | Java 17 (projekt nie zawiera kodu Kotlin) |
| Android Gradle Plugin | 8.3.2 |
| Gradle | 8.5 |
| compileSdk / targetSdk | 36 |
| minSdk | 24 (Android 7.0) |
| Biblioteki UI | brak — wyłącznie platformowe API |
| Kody QR | ZXing 3.5.3 (lokalny JAR w `core/libs/`) |
| Backend | Supabase (REST) |
| Minifikacja | R8 + `shrinkResources` w wersji release |

---

## 🔨 Budowanie ze źródeł

### Wymagania

- **JDK 17** lub nowszy
- **Android SDK** z platformą API 36 i build-tools 35.0.0
- Połączenie z internetem przy pierwszym budowaniu (pobranie Gradle i wtyczek)

### Konfiguracja

Utwórz plik `local.properties` w katalogu głównym (nie jest wersjonowany):

```properties
sdk.dir=/sciezka/do/Android/Sdk

# Podpisywanie wersji release
DROPME_STORE_FILE=release.keystore
DROPME_STORE_PASSWORD=twoje-haslo
DROPME_KEY_ALIAS=dropme
DROPME_KEY_PASSWORD=twoje-haslo

# Opcjonalne: publikacja karty online (bez tego klucza funkcja jest wyłączona)
DROPME_TELEGRAPH_TOKEN=twoj-token
```

Wszystkie wartości można też przekazać przez zmienne środowiskowe o tych samych nazwach.
Bez konfiguracji podpisywania zbuduje się nadal — wersja debug dostanie wtedy
domyślny klucz debug.

### Komendy

```bash
./gradlew test              # testy jednostkowe
./gradlew assembleDebug     # APK debug
./gradlew assembleRelease   # APK release (podpisany, zminifikowany)
```

Gotowe pliki:

```text
app/build/outputs/apk/release/app-release.apk
app/build/outputs/apk/debug/app-debug.apk
```

#### Build do publikacji

Plik dołączony do repozytorium jest budowany **bez prywatnych tokenów**, żeby pobrany
APK nie wynosił żadnego sekretu na zewnątrz. Nadpisujesz je z linii komend, bez ruszania
swojego `local.properties`:

```bash
./gradlew assembleRelease -PDROPME_TELEGRAPH_TOKEN=
cp app/build/outputs/apk/release/app-release.apk .
```

Bez tego nadpisania lokalny build zawiera token i funkcja karty online działa normalnie.

### Testy

Testy jednostkowe znajdują się w `core/src/test/java/` i pokrywają logikę, która
najłatwiej psuje się po cichu:

- rozpoznawanie platformy z linku i budowa adresu profilu z nicku,
- hashowanie i weryfikacja haseł oraz PIN-ów (w tym zgodność ze starym formatem),
- reguły rang (Creator / Admin / Premium / Gość),
- zapis i odczyt modelu użytkownika do JSON,
- generowanie vCard.

```bash
./gradlew test
```

---

## 🔐 Bezpieczeństwo

**Co jest już zrobione:**

- Hasła i PIN-y są hashowane z losową solą (PBKDF2-HMAC-SHA256, 20 000 iteracji)
  — nigdy nie trafiają do bazy ani do pamięci telefonu w postaci jawnej.
- Sekrety (hasła keystore'a, tokeny) trzymane są w `local.properties` i zmiennych
  środowiskowych, poza repozytorium.
- Klucz podpisywania, pliki APK z danymi debugowania i prywatne załączniki są
  wykluczone przez `.gitignore`.
- Rozmiar kodu ogranicza powierzchnię ataku: ~169 kB APK bez zewnętrznych bibliotek UI.
- Dołączony APK jest budowany bez prywatnych tokenów — patrz „Build do publikacji”.

**Wymaga dokończenia przed publicznym wdrożeniem:**

- **Konfiguracja RLS w bazie.** Reguły dostępu muszą być wymuszone po stronie Supabase,
  a nie tylko w kodzie aplikacji — inaczej klucz API z APK pozwala modyfikować dane
  innych użytkowników.
- **Uprawnienia po stronie serwera.** Sprawdzanie rang (kto może nadać Premium lub Admina)
  powinno działać w bazie jako polityka, nie tylko w kliencie.
- **Rotacja kluczy** przed upublicznieniem repozytorium, jeśli klucze były już
  gdziekolwiek publikowane.

Szczegóły tych punktów i pozostałe zadania rozwojowe znajdują się w sekcji poniżej.

---

## 🗺️ Plan rozwoju

- [ ] Polityki RLS i uprawnienia rang po stronie bazy
- [ ] Kody QR w chmurze (teraz są tylko lokalne) — eksport i import danych
- [ ] Automatyczne potwierdzanie wsparcia zamiast ręcznej weryfikacji
- [ ] Teksty interfejsu wyniesione do `strings.xml` (obecnie częściowo w layoutach)
- [ ] Katalog wersji (`libs.versions.toml`) dla zależności
- [ ] Więcej testów dla logiki uprawnień i synchronizacji

---

## 📄 Licencja

Projekt udostępniony do wglądu i użytku własnego. Kod źródłowy możesz przeglądać,
budować i modyfikować na własne potrzeby.
