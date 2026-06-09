# App Library Research

Date: 2026-06-07

Rule: use a proven Android/platform/library solution when it is compatible and useful; otherwise implement the smallest robust in-house version and document why.

## Selected Libraries And APIs

| Area | Tool/API | URL/source | License | What it can do | Android app use | Recommendation | Reason |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Tuner pitch detection | TarsosDSP | https://github.com/JorenSix/TarsosDSP | GPL-3.0 | Java DSP framework with YIN, McLeod, dynamic wavelet pitch detectors | Technically yes | do not use as dependency | Useful reference, but GPL-3.0 is not a safe fit for this app unless the whole license strategy changes. |
| Tuner pitch detection | YIN algorithm | https://audition.ens.fr/adc/pdf/2002_JASA_YIN.pdf | Algorithm; implementation license depends on code | Monophonic pitch detection with confidence threshold | yes | use in-house | Proven algorithm; can be implemented locally over PCM buffers without GPL code. |
| Tuner/sound meter input | Android `AudioRecord` | https://developer.android.com/reference/android/media/AudioRecord | Android SDK | Reads microphone PCM buffers | yes | use | Required local microphone path for tuner and approximate sound meter. |
| Sound meter | RMS/amplitude over PCM | Android SDK / standard DSP | Algorithmic | Calculates amplitude and approximate dB relative to local calibration | yes | use in-house | No fake SPL; label as approximate unless calibrated. |
| Metronome | `SystemClock.elapsedRealtime()` + handler thread | https://developer.android.com/reference/android/os/SystemClock | Android SDK | Monotonic elapsed time scheduling | yes | use | Allows drift-compensated beat scheduling independent of wall-clock jumps. |
| Metronome audio | `ToneGenerator` / generated `AudioTrack` tick | https://developer.android.com/reference/android/media/ToneGenerator | Android SDK | Short audible tones | yes | use simple `ToneGenerator` first | Good enough for optional tick; avoid heavy native audio engine in this pass. |
| Weather | Open-Meteo forecast/geocoding | https://open-meteo.com/ and https://github.com/open-meteo/open-meteo | Data CC BY 4.0; API free/no-key for non-commercial use; server AGPL if self-hosted | Current weather and geocoding JSON | yes | use | Best no-key provider; cache last result and add attribution docs. |
| Weather HTTP | OkHttp `4.12.0` | https://github.com/square/okhttp | Apache-2.0 | HTTP client/cache | yes | use | `5.3.2` was requested, but it is compiled with Kotlin metadata 2.2 and fails this Kotlin 2.0.21 build; `4.12.0` keeps the same HTTP role without forcing a project-wide toolchain upgrade. |
| Web portal | NanoHTTPD `2.3.1` | https://github.com/NanoHttpd/nanohttpd | BSD-3-Clause / Modified BSD | Small embeddable Java HTTP server | yes | use | Better fit than Ktor for a small LAN editor/status portal. |
| Web portal alternative | Ktor server | https://ktor.io/ | Apache-2.0 | Full Kotlin server framework | possible | do not use now | Too heavy for disabled-by-default local portal. |
| JSON | `org.json` / current parsers | Android SDK | Android SDK | Parse small weather/import/export payloads | yes | use now | Avoid adding serialization plugin churn in this pass; can revisit `kotlinx.serialization`. |
| Sensor orientation | Android rotation vector | https://developer.android.com/reference/android/hardware/SensorManager | Android SDK | Converts rotation vector to matrix/orientation | yes | use | Preferred over raw orientation; already available in sensor path. |
| Step/motion | `TYPE_STEP_DETECTOR` / `TYPE_STEP_COUNTER` | https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion | Android SDK; requires `ACTIVITY_RECOGNITION` on Android 10+ | Real step events/counter | yes | use if available | Real steps only; otherwise show motion intensity fallback, not fake steps. |
| Connectivity | `ConnectivityManager.NetworkCallback` | https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback | Android SDK | Network state callbacks | yes | use | Supports network status and portal availability; Wi-Fi info can be location-sensitive. |
| Notification Pulse | `NotificationListenerService` | https://developer.android.com/reference/android/service/notification/NotificationListenerService | Android SDK | Notification callbacks after explicit user permission | yes | use opt-in | Privacy-sensitive; only enabled with clear setup and app filters. |
| Schedule/wake | `AlarmManager` | https://developer.android.com/develop/background-work/services/alarms | Android SDK | Wake/schedule broadcasts, inexact/exact with OS limits | yes | use carefully | Use custom schedule; document exact-alarm restrictions. |
| Payment/NFC | Android NFC tag dispatch | https://developer.android.com/develop/connectivity/nfc/nfc | Android SDK | Tag/NDEF/tech/tag-discovered intents | yes | maybe | Can trigger wallet-active/payment visual, not confirmed transaction success. |
| Wallet foreground | `UsageStatsManager` | https://developer.android.com/reference/android/app/usage/UsageStatsManager | Android SDK; user grants Usage Access | Foreground usage events | yes | maybe | Setup-gated and not perfectly reliable; never claim transaction detection. |

## Feature Decisions

### Tuner

Decision: implement `PitchDetector` in-house using YIN-style difference/cumulative mean normalized difference over mono PCM from `AudioRecord`. Use RMS gate, confidence threshold, median/low-pass smoothing, instrument presets, A4 reference, and tolerance settings. Do not keep manual cents as normal behavior.

### Sound Level Meter

Decision: implement shared microphone engine plus RMS and peak hold. Convert to approximate dB using a local reference offset and show "approx" in docs/UI. Do not claim calibrated SPL.

### Metronome

Decision: use a handler thread and `SystemClock.elapsedRealtime()` target-beat timestamps. On every beat, render a Matrix beat frame, optionally vibrate, optionally play a short tone. Correct drift by scheduling the next absolute beat, not `now + interval` after work completes.

### Weather

Decision: use Open-Meteo geocoding and forecast current weather through OkHttp. Cache last successful result with timestamp in preferences. If offline or fetch fails, show cached result with degraded readiness; otherwise show setup/error.

### Wi-Fi Web Portal

Decision: use NanoHTTPD as a disabled-by-default local server. Generate a random PIN/token on enable. Bind to local interfaces, expose status/toys/activate/deactivate/assets/test-frame/logs/settings routes, and serve a static 13x13 editor. Reject unauthenticated requests. No arbitrary code/plugin execution.

### Payment / Wallet / NFC

Decision: implement manual payment visual trigger immediately. Optional NFC trigger uses Android NFC intents if the app receives a real tag event. Optional Wallet foreground detection uses Usage Access and is labelled "Wallet active", not "paid". Success checkmark is only manual/test/NFC event, never inferred payment success.

### Notification Pulse

Decision: implement only opt-in notification listener service. Until permission is granted and filters are configured, visibility is `NEEDS_SETUP`; if not implemented, keep future/hidden.

### Schedule / Night / Alarm

Decision: implement GlyphHub custom schedule policy in app settings and enforce it in the service/status router. Custom wake schedule can use `AlarmManager`; system next alarm is documented as optional/future unless accessible and tested.

### Sensor Tools

Decision: reuse Android rotation vector, accelerometer, light sensor, step detector/counter, and the existing `accuracy/` package. Add shared readiness/capability checks so modules with missing sensors are `NEEDS_SETUP` or degraded, not fake.

### Pixel / Animation Editor

Decision: continue using canonical 13x13 JSON. Bitmap and Glyph Matrix Editor/Toyph-style importers are usable; Lottie remains unsupported until a real dependency-backed importer is added.

## Dependency Additions

Add:

- `implementation("com.squareup.okhttp3:okhttp:4.12.0")`
- `implementation("org.nanohttpd:nanohttpd:2.3.1")`

Do not add:

- TarsosDSP or Android-TarsosDSP forks, because GPL-3.0 is not acceptable for this pass.
- Ktor server, because it is heavier than the required portal.
- Lottie Android, unless a later pass implements and tests rasterized import.

## Validation Requirements

- Build and assemble after implementation.
- Microphone tools require live RECORD_AUDIO permission and visible input reaction before being called working.
- Weather requires one successful fetch/cache/offline test before being called working.
- Web portal routes require an HTTP request test with token before being called working.
- Status layer features require logcat evidence and manual Matrix visual confirmation for production claims.
