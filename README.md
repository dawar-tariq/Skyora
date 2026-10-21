# SKYORA

**Weather for a better tomorrow.**

A native Android weather app built with Kotlin, Jetpack Compose and Material 3. Midnight-navy surfaces, cyan accents, hand-drawn weather glyphs and a custom design system — powered by the free [Open-Meteo](https://open-meteo.com/) forecast and geocoding APIs.

---

## Status

| | |
|---|---|
| Source | Complete: 66 app Kotlin sources + 3 test files, 8 screens, 8 ViewModels, 3 repositories, 338 strings |
| Android build | **Not executed in the authoring environment** (no Android SDK / Google Maven access). See [Verification](#verification) for exactly what was and was not run. |
| Unit tests | 27 JVM tests in `app/src/test`; their assertions were executed in a JVM harness against the compiled domain/data classes and all passed. |
| Notifications | Deliberately out of scope for this release; the seam exists (`WeatherAlertScheduler` + `NoOpWeatherAlertScheduler`). |

Do not treat this repository as a tested APK. Treat it as complete, reviewed source with a verified pure-Kotlin core.

---

## Screens

| Screen | Highlights |
|---|---|
| **Splash** | Android 12+ splash screen API with the same mark as the launcher icon, followed by an ~850 ms in-app brand reveal while the onboarding flag is read. No artificial loading delay. |
| **Onboarding** | Three pages (forecast, location, saved cities) with progress dots, Next/Skip and Get Started. Completion is persisted in DataStore. |
| **Location permission** | Permission is requested only when the user taps *Use my location*. Manual city search is always available. Denial, approximate (coarse) permission and revocation are handled, with a route to system settings after permanent denial. |
| **Home** | Selected city and country, local date/time, hero temperature, condition, feels-like, high/low, 24-hour horizontal strip, 7-day list with ranges, wind/humidity/pressure/UV/visibility metrics, pull-to-refresh, cached-data banner, bottom navigation. |
| **Search** | 350 ms debounced autocomplete against Open-Meteo geocoding, clear button, persisted recent searches (max 6, de-duplicated), popular-city tiles, loading skeleton, no-results and network-error states with retry, geocoding attribution. |
| **City weather** | Full forecast for any city reached from search or saved cities, back navigation, add/remove saved city, "set as home city". Selecting a city here never overwrites the device location. |
| **Saved cities** | Current temperature and condition per city, add/remove (with undo), reorder, tap to open, persisted with stable IDs, pull-to-refresh, empty and partial-failure states. |
| **Settings** | Celsius/Fahrenheit, automatic location toggle with permission handling, home city, permission status, notification placeholders ("coming later"), cache and recent-search clearing, Open-Meteo attribution/terms, About, Privacy, reset onboarding. |

All screens are edge-to-edge (`enableEdgeToEdge` + insets), scroll-safe under the floating bottom bar (`LocalBottomBarInset`), and built from the same tokens, so spacing and typography stay consistent from a 5" phone to a tablet-class display (`maxContentWidth = 640.dp`).

---

## Architecture

```
UI (Compose)          screens + design system; no network or disk access
   │  collects StateFlow, sends events
ViewModel             immutable UiState, debouncing, error mapping, one-shot events
   │  suspend calls / Flows
Domain                models, repository interfaces, LocationCoordinator
   │
Data                  Retrofit APIs, DTOs + mappers, DataStore repositories, cache
```

* **Immutable UI state** — every screen renders a single `data class …UiState`; collections use `collectAsStateWithLifecycle()`.
* **Single source of truth** — weather snapshots live in a repository-level cache (`StateFlow` per location); saved cities, preferences and recents live in DataStore.
* **Offline cache** — the last successful response per location is persisted (max 10 entries). A snapshot younger than 5 minutes is served without a network call; older snapshots are refreshed silently and kept if the refresh fails, with a "last updated" banner.
* **No duplicate work** — manual and automatic refreshes share one in-flight path; search is debounced and de-duplicated with `distinctUntilChanged`; geocoding results are cached (24 entries, 5 minutes).
* **Separated concepts** — device location (`gps:`), geocoded city (`geo:`), currently selected city, default/home city and saved cities are distinct models. Selecting a city only changes `selectedLocation`; it never rewrites the GPS fix.
* **Typed failures** — `WeatherError` (`Offline`, `Timeout`, `Server`, `InvalidData`, `Unknown`) crosses the repository boundary; raw exceptions never reach a composable.
* **Stable location identity** — geocoded IDs are `geo:<open-meteo id>`, device IDs are `gps:<lat>,<lon>` (3 decimals). `isSamePlaceAs` treats two locations within 3 km as the same place, so a GPS fix and a searched city cannot both appear in recents or favourites.

### Packages

```
com.skyora.weather
├── MainActivity, SkyoraApplication
├── di/AppContainer              manual DI, lazy singletons
├── core/
│   ├── network/NetworkMonitor   connectivity as a Flow
│   └── util/                    formatters, clock, permissions, links, route codec
├── domain/
│   ├── model/                   Location, WeatherSnapshot, UserPreferences, Result types
│   ├── repository/              Weather, Geocoding, Preferences, SavedCities, DeviceLocation
│   ├── location/LocationCoordinator
│   └── notifications/           WeatherAlertScheduler (extension seam)
├── data/
│   ├── remote/                  Retrofit services + @Serializable DTOs
│   ├── mapper/ForecastMapper    WMO codes, timezone handling, plausibility checks
│   ├── local/                   DataStore preferences, saved cities, weather cache
│   ├── repository/              implementations + error mapping
│   └── location/                Android fused/GPS provider
└── ui/
    ├── theme/                   colours, Manrope typography, shapes, dimens
    ├── designsystem/            cards, controls, glyphs, backdrop, dialogs, nav, feedback
    ├── weather/                 shared forecast sections and state widgets
    ├── navigation/SkyoraApp     single NavHost + floating bottom bar
    └── splash, onboarding, permission, home, search, city, saved, settings
```

---

## Data & API

Two Open-Meteo endpoints, no API key:

* `GET https://api.open-meteo.com/v1/forecast`
  `current` (temperature, humidity, apparent temperature, is_day, precipitation, weather code, cloud cover, pressure, wind speed/direction/gusts), `hourly` (temperature, apparent temperature, humidity, weather code, precipitation probability, is_day, visibility), `daily` (weather code, max/min, sunrise/sunset, UV max, precipitation probability max, wind max), `timezone=auto`, `forecast_days=7`, `forecast_hours=25`, metric units.
* `GET https://geocoding-api.open-meteo.com/v1/search?name=…&count=10&language=en&format=json`

Mapping rules:

* Values are stored in Celsius internally; the C/F preference is applied only at display time (`TemperatureUnit.convertFromCelsius`, `formatTemperature`).
* The API's `utc_offset_seconds` and `timezone` drive all local times; a missing or unrecognised zone falls back to a fixed offset (`resolveZoneId`).
* WMO weather codes map to nine conditions (`WeatherCondition`) with separate day/night glyphs.
* Nulls, non-finite numbers and implausible temperatures (−90…60 °C) are dropped. If current conditions or a complete forecast day are missing, the payload is rejected as `InvalidData` rather than rendering fake zeros.

### Attribution and licensing — read before releasing

* Forecast data: **CC BY 4.0**, attribution required — “Weather data by Open-Meteo.com”, linked to <https://open-meteo.com/> and the licence.
* Geocoding data: **CC BY-NC 4.0**, with a link next to displayed location results.
* The free Open-Meteo tier is **for non-commercial use** (limits at the time of writing: 600 calls/minute, 5,000/hour, 10,000/day, 300,000/month). Commercial distribution requires a paid Open-Meteo plan and an API key; this app neither ships nor claims such a licence.
* Attribution is shown in Settings (Licence modal), on the Home footer, and in the search results footer. The links live in `core/util/Links.kt`.
* Typeface: Manrope, SIL Open Font License — `licenses/Manrope-OFL.txt`.

---

## Privacy

No account, no analytics, no ads. Coordinates are sent only to Open-Meteo to fetch weather and to geocode typed city names. Preferences, saved cities, recent searches and the weather cache stay on the device in DataStore. Location permission is optional and can be revoked at any time; the app falls back to the saved city.

---

## Build & run

Requirements: JDK 17, Android Studio Ladybug (or newer) with Android SDK 35, or a command-line SDK installation.

```bash
# Android SDK path
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/
./gradlew test                   # JVM unit tests
./gradlew installDebug           # install on a connected device/emulator
```

* `applicationId`: `com.skyora.weather` (debug builds append `.debug`)
* `minSdk 26`, `compileSdk`/`targetSdk 35`
* Kotlin 2.0.21 · AGP 8.7.3 · Compose BOM 2024.12.01 · Material 3 · Navigation Compose 2.8.5
* Release builds enable R8 and resource shrinking; `app/proguard-rules.pro` keeps kotlinx-serialization serializers for Retrofit.
* No API key or `local.properties` secret is required.

### Troubleshooting

* **Gradle cannot reach Google Maven** — the build needs `google()` and `mavenCentral()`; offline environments cannot resolve Compose.
* **Fonts** — Manrope is bundled in `res/font`; no downloadable fonts are used.
* **Map/locale** — date and time formatting use the device locale; timezones come from the API, not the device.

---

## Verification

What was actually checked in the authoring sandbox:

* All 66 Kotlin files parse without syntax errors (tree-sitter Kotlin grammar).
* Every `R.string.*`, colour, font, drawable, mipmap and icon reference resolves to a declared resource or a `material-icons-extended` symbol.
* Cross-file import/declaration audit (including composable signatures, named arguments and ViewModel members) reports no unresolved project symbols.
* Domain and data layers were compiled with the Kotlin JVM compiler and executed through a standalone harness (~90 assertions): formatting, location identity and route encoding, DTO → snapshot mapping, cache hits/force refresh/timeouts, geocoding dedupe and cache, preferences, saved-city ordering/removal/undo, cache pruning and the location coordinator.
* The 27 unit tests in `app/src/test` were compiled and executed against that same compiled code with a JUnit-compatible harness: **all passed**.
* Bugs found and fixed during review include a wrong hourly index for “current visibility”, a missing `LaunchedEffect` import, a token used from the wrong design-system object, auto-mirrored icon imports, and a `kotlin { }` block nested inside `android { }` in the build script.

What was **not** done: a real Gradle/Android build, Compose rendering or screenshot tests, instrumentation tests, and any on-device run. Compose APIs follow stable Material 3 / Compose 1.7 signatures, but only compilation on a machine with the Android SDK can prove the UI layer builds.

---

## Limitations and next steps

* No push notifications, widgets, or background refresh in this phase. `WeatherAlertScheduler` is the intended entry point for a WorkManager-based implementation.
* No release signing config; add a keystore before distributing.
* Localisation: English strings only (all copy is in `strings.xml`, ready for translation).
* Commercial release requires an Open-Meteo paid plan (see above).
* Possible additions: hourly detail sheet, sunrise/sunset timeline, air quality (a separate Open-Meteo API), tablet two-pane layout.

---

## License

Source © the repository owner. Weather data © Open-Meteo.com contributors, licensed CC BY 4.0; geocoding data CC BY-NC 4.0. Manrope is licensed under the SIL Open Font License 1.1.
