# SKYORA

**Weather for a better tomorrow.**

A native Android weather app built with Kotlin, Jetpack Compose and Material 3. Midnight-navy surfaces, cyan accents, hand-drawn weather glyphs and a custom design system — powered by the free [Open-Meteo](https://open-meteo.com/) forecast and geocoding APIs.

---

## Status

| | |
|---|---|
| Source | 80 Kotlin sources (74 app + 6 test), 8 screens, 397 strings, 20 weather-icon vectors |
| Android build | **Verified on GitHub Actions** (ubuntu-latest, JDK 17, Android SDK 35): `testDebugUnitTest` and `assembleDebug` both pass (run [39571962863](https://github.com/dawar-tariq/Skyora/actions/runs/39571962863)). |
| APK | Built by CI and published to [Releases](https://github.com/dawar-tariq/Skyora/releases/latest) (debug-signed, installable). |
| Unit tests | **51 JVM tests, 0 failures**, executed by CI (`unit-tests` commit status carries the count). |
| Widgets | 2x2 compact and 4x2 forecast home-screen widgets with per-widget city selection. |
| Notifications | Local WorkManager-scheduled daily summary and rain alerts (not remote push). Severe-weather alerts are explicitly unavailable rather than fabricated. |

## Round 2 changes at a glance

| Area | Files |
|---|---|
| Day/night logic | `domain/model/SkyIcons.kt` (new), `domain/model/WeatherSnapshot.kt` (`sunTimes()`), `data/mapper/ForecastMapper.kt` (recomputes `isDay` per hour) |
| Icon artwork + mapping | `res/drawable/weather_*.xml` (20 new), `ui/designsystem/WeatherIcon.kt` (new), `WeatherVisuals.kt` (`weatherIconRes` overloads), `WeatherSections.kt`, `SavedCitiesScreen.kt`, `Illustrations.kt`; removed `WeatherGlyph.kt` |
| Brand | `res/drawable-*/ic_brand_mark.png`, `ic_splash_logo.png`, `ic_launcher_{foreground,background}.png`, `mipmap-*/ic_launcher*.png`, `mipmap-anydpi-v26/ic_launcher*.xml`, `drawable/ic_launcher_monochrome.xml`, `values/themes.xml`, `ui/designsystem/Brand.kt` |
| Home polish | `ui/home/HomeScreen.kt`, `ui/weather/WeatherSections.kt` (row weights, ellipsis, padding above the bottom bar) |
| Widgets | `widget/` (5 files), `work/WidgetRefreshWorker.kt`, `res/layout/widget_compact.xml`, `widget_forecast.xml`, `activity_widget_config.xml`, `res/xml/widget_*_info.xml`, `res/drawable-nodpi/widget_preview_*.png`, `widget_background.xml`, `widget_chip_background.xml` |
| Notifications | `notifications/` (4 files), `work/DailyForecastWorker.kt`, `work/RainCheckWorker.kt`, `domain/notifications/RainAlertPolicy.kt`, `drawable/ic_stat_weather.xml` |
| Settings | `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, `ui/designsystem/Dialogs.kt` (`extraContent`), `Controls.kt` (composable segment labels), `core/util/ContextExt.kt` |
| Plumbing | `AndroidManifest.xml` (widget receivers/activity, `POST_NOTIFICATIONS`), `di/AppContainer.kt`, `SkyoraApplication.kt`, `MainActivity.kt` (widget/notification tap → city), `ui/navigation/SkyoraApp.kt`, `data/repository/WeatherRepositoryImpl.kt` (`onSnapshotStored`), `data/local/PreferencesRepositoryImpl.kt`, `domain/model/UserPreferences.kt`, `domain/repository/Repositories.kt`, `gradle/libs.versions.toml` + `app/build.gradle.kts` (WorkManager 2.9.1) |
| Tests | `app/src/test/.../SkyIconsTest.kt`, `RainAlertPolicyTest.kt`, `NotificationSchedulerTest.kt`, extended `ForecastParsingTest.kt` |

## Get the app

**Direct download (always the latest `main` build):**

<https://github.com/dawar-tariq/Skyora/releases/latest/download/app-debug.apk>

Or open the [latest release](https://github.com/dawar-tariq/Skyora/releases/latest). The APK is rebuilt by CI on every push to `main`; it is **debug-signed**, ~18 MB, and installs on Android 8.0 (API 26) or newer. Enable "install unknown apps" for your browser or file manager when prompted.

Prefer the Actions tab? Every run also uploads a `skyora-debug-apk` artifact (zip containing the same APK).

Or build it yourself with the commands in [Build & run](#build--run).

---

## Screens

| Screen | Highlights |
|---|---|
| **Splash** | Android 12+ splash screen API (`Theme.SplashScreen`, `ic_splash_logo`) using the same crescent-and-cloud mark as the launcher icon, followed by a short in-app brand reveal while the start destination is read. No artificial loading delay and only one activity. |
| **Onboarding** | Three pages (forecast, location, saved cities) with progress dots, Next/Skip and Get Started. Completion is persisted in DataStore. |
| **Location permission** | Permission is requested only when the user taps *Use my location*. Manual city search is always available. Denial, approximate (coarse) permission and revocation are handled, with a route to system settings after permanent denial. |
| **Home** | Selected city and country, local date/time, hero temperature, condition, feels-like, high/low, 24-hour horizontal strip (each hour with its own day/night artwork), 7-day list with ranges, wind/humidity/pressure/UV/visibility metrics, sunrise/sunset, pull-to-refresh, cached-data banner, bottom navigation. The whole screen scrolls; content ends above the floating bottom bar. |
| **Search** | 350 ms debounced autocomplete against Open-Meteo geocoding, clear button, persisted recent searches (max 6, de-duplicated), popular-city tiles, loading skeleton, no-results and network-error states with retry, geocoding attribution. |
| **City weather** | Full forecast for any city reached from search or saved cities, back navigation, add/remove saved city, "set as home city". Selecting a city here never overwrites the device location. |
| **Saved cities** | Current temperature and condition per city, add/remove (with undo), reorder, tap to open, persisted with stable IDs, pull-to-refresh, empty and partial-failure states. |
| **Settings** | Celsius/Fahrenheit, automatic location toggle with permission handling, home city, permission status, **weather notifications** (master switch, daily summary + delivery time, rain alerts + 40/60/80 % threshold, forecast city, `POST_NOTIFICATIONS` rationale and settings shortcut), severe weather marked unavailable, cache and recent-search clearing, Open-Meteo attribution/terms, About, Privacy, reset onboarding. |

All screens are edge-to-edge (`enableEdgeToEdge` + insets), scroll-safe under the floating bottom bar (`LocalBottomBarInset`), and built from the same tokens, so spacing and typography stay consistent from a 5" phone to a tablet-class display (`maxContentWidth = 640.dp`).

### Brand assets

The launcher icon is the supplied crescent-and-cloud mark, not a re-drawn substitute: a full-bleed adaptive foreground (288/432/576 px for xhdpi/xxhdpi/xxxhdpi) inside a deep-navy `ic_launcher_background` layer with the mark kept inside the 66 dp safe zone, plus legacy mipmaps (48–192 px, square and round) for launchers without adaptive icons and a monochrome layer for themed icons. The same artwork is exported as `ic_brand_mark` (64–256 px) for the in-app logo, widget header and notification large icon, and as `ic_splash_logo` for the Android 12 splash screen. No text is baked into any icon, and the old vector launcher/splash drawables were deleted so nothing stale can be picked up.

### Weather icons: day/night is computed, never guessed

`domain/model/SkyIcons.kt` is the single source of truth for artwork. For every forecast item it:

1. takes that item's **own** timestamp,
2. converts it to the location's timezone (`snapshot.zoneId()`, from the API's `timezone`/`utc_offset_seconds`),
3. looks up that **calendar day's** sunrise/sunset (plus the neighbouring days, so polar windows that spill across midnight still work),
4. falls back to sunrise + 12 h / sunset − 12 h when only one value is present, and to 06:00–18:00 local when the payload has no sun data at all.

Clear, mainly-clear, partly-cloudy and rain-shower artwork therefore flips at the real daylight boundary of that hour — a 15:00 slot can no longer show a moon. Precipitation, fog and storm icons are intentionally time-independent. The API's `is_day` flag is ignored for hourly items (it is a single payload-wide value), which was the root cause of the original bug.

`ui/designsystem/WeatherIcon.kt` maps the resulting key (`weather_clear_day`, `weather_rain`, …) to one of **20 vector drawables** covering every Open-Meteo WMO code (0, 1, 2, 3, 45, 48, 51–57, 61–67, 71–77, 80–82, 85–86, 95, 96, 99) plus a neutral fallback, and the same function is used by Compose screens, the home-screen widgets and notifications.

### Home-screen widgets

| Widget | Size | Contents |
|---|---|---|
| **SKYORA · Current** | 2x2 (resizable) | brand mark, city, current temperature, condition, day/night-aware icon, high/low when the cell is tall enough, "Updated … ago" |
| **SKYORA · Forecast** | 4x2 (resizable) | city, temperature, condition, high/low, icon, next 3–4 hours (each with its own day/night icon and local time), "Updated … ago" |

Both are `RemoteViews` layouts (LinearLayout/TextView/ImageView only) built by `widget/WidgetRenderer.kt` from the **existing** repository — the widgets never call the network directly. Data comes from the cache when present and from a normal repository fetch otherwise, so a widget and the app can never disagree. Each widget stores its own city (`WidgetPreferences`) chosen in `WidgetConfigActivity` (the `android:configure` activity); cancelling removes the widget. The four refresh triggers are: widget placed/resized, any successful app fetch, a change of selected city/temperature unit, and a 60-minute periodic safety net (`WorkManager`, all coalesced under unique work names). Deleting the last widget cancels the periodic job, and re-placing one re-arms it. Both widgets ship a picker preview (`previewLayout` on Android 12+, `previewImage` below).

### Notifications (local schedule, not push)

`notifications/NotificationScheduler.kt` owns two WorkManager schedules, each under a unique name, so changing a setting replaces instead of duplicating work:

* **Daily summary** — one-time work re-armed after each run at the user's chosen wall-clock time (`delayUntil` handles "already passed today"), network-constrained, re-read from preferences at run time. Shows today's condition, high/low and the correct icon for the city in use; dedupe key is `location|<date>`.
* **Rain alerts** — 30-minute periodic work (WorkManager's practical minimum for this pattern). It reads the `precipitation_probability` of upcoming hours, compares it with the configured 40/60/80 % threshold inside the configured 6-hour horizon, and posts **at most one alert per forecast hour** (`location|epochHour`). It never infers rain from the weather code.
* **Severe weather** — **not implemented and not faked.** No official alert feed is integrated, so the Settings row states that instead of offering a switch. There is no Firebase, no server and no push token anywhere in the project.

Both notification types are suppressible, open the relevant city on tap (`MainActivity.EXTRA_LOCATION_PAYLOAD` → `SkyoraApp`), and are skipped entirely when notifications are disabled or the runtime permission is denied — the rest of the app keeps working.

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
│   ├── model/                   Location, WeatherSnapshot, SkyIcons (day/night), UserPreferences
│   ├── repository/              Weather, Geocoding, Preferences, SavedCities, DeviceLocation
│   ├── location/LocationCoordinator
│   └── notifications/           WeatherAlertScheduler seam + RainAlertPolicy (pure)
├── notifications/               channels, notifier, dedupe store, WorkManager scheduler
├── widget/                      providers, RemoteViews renderer, per-widget city store
├── work/                        daily forecast, rain check, widget refresh workers
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
* WMO weather codes map to sixteen `WeatherCondition` values, which resolve to 20 icon drawables; day/night is derived per item from that item's timestamp (see “Weather icons” above), not from the API's `is_day` flag.
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

**Continuous integration (the authoritative check):** `.github/workflows/android.yml` runs on every push. It installs JDK 17 and Android SDK 35, then runs `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`, publishing the APK and build logs as artifacts and the unit-test count as a commit status.

| Run | Result |
|---|---|
| [39571962863](https://github.com/dawar-tariq/Skyora/actions/runs/39571962863) — head of this branch | `testDebugUnitTest` **51 passed, 0 failed**, `assembleDebug` **success**, debug APK uploaded |

Round-2 issues found by the real build and fixed (in this order): duplicate `action_*` strings, unescaped apostrophes in two notification strings (aapt "invalid unicode escape sequence"), missing `Box`/`matchParentSize` imports in `Brand.kt`, missing dialog/icon-button imports in `SettingsScreen.kt`, an out-of-scope `zone` in `WeatherHero`, and `Notification.CATEGORY_WEATHER`, which does not exist on the platform.

Static checks performed while the SDK was unavailable locally:

* All 80 Kotlin files pass a brace/parenthesis/bracket balance check (string- and comment-aware).
* Every `R.string`/`R.color`/`R.drawable`/`R.layout`/`R.id`/`R.xml` reference resolves, and every `@drawable|color|string|style` reference inside `res/` resolves; all resource XML parses.
* Every project-internal import resolves to a declaration in that package; every `Icons.Outlined.*` use has a matching import.
* All 20 weather vectors were rasterised (resvg) and inspected: distinct day sun, night crescent, sun/moon-behind-cloud, rain, drizzle, snow, sleet, storm with lightning, hail, fog and fallback artwork, with consistent stroke and scale.
* Widget layouts were audited for `RemoteViews` compatibility (no `Space`, `View`, or `ConstraintLayout`); the compact widget's optional line is dropped by measured width **and** height instead of clipping.

What is **not** verified: no emulator/device run and no screenshot or instrumentation tests in this environment. Widget rendering, notification delivery timing and the installed launcher icon have therefore not been visually exercised on hardware; they are compiled and CI-built, not device-confirmed.

## Limitations and next steps

* Notifications and widgets are **locally scheduled**; there is no remote push (Firebase) and no server-side component. WorkManager timing is best-effort under Doze/battery saver.
* Severe-weather alerts require an official, attributable warning feed; until one is integrated the feature is marked unavailable.
* No release signing config; add a keystore before distributing.
* Localisation: English strings only (all copy is in `strings.xml`, ready for translation).
* Commercial release requires an Open-Meteo paid plan (see above).
* Possible additions: hourly detail sheet, air quality (a separate Open-Meteo API), tablet two-pane layout, additional widget sizes.

## License

Source © the repository owner. Weather data © Open-Meteo.com contributors, licensed CC BY 4.0; geocoding data CC BY-NC 4.0. Manrope is licensed under the SIL Open Font License 1.1.
