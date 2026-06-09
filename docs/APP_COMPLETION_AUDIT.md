# App Completion Audit

Date: 2026-06-07

Target: Nothing Phone (4a) Pro / A069P / 13x13 Glyph Matrix through the official Glyph Toy service.

Status values:

- `complete`: compiled, wired, and already useful; may still need physical visual validation.
- `partial`: real code exists, but requested behavior is incomplete.
- `placeholder`: visible behavior is not production-useful yet.
- `hidden_experimental`: intentionally hidden unless experimental mode is enabled.
- `missing`: no module or functional system layer exists yet.
- `broken`: implemented but known not to work.

## Core Apps

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Dice | partial | `DiceToyModule.kt`, shared glyph icon library | Configurable dice, shake/tap roll, smooth settle, validated readable faces | no | implement in-house using shared assets | build, debug preview, phone Matrix visual check | Keep visible as `READY`; validate on hardware and tune high-side numeric layout. |
| Coin | partial | `CoinToyModule.kt`, shared glyph icon library | Shake/tap flip with readable non-label outcome glyphs where possible | no | implement in-house using shared assets | build, debug preview, phone Matrix visual check | Keep visible as `READY`; replace label-heavy outcomes with stronger glyphs. |
| Clock | partial | `ClockToyModule.kt` | Readable digital/analog clock, AOD-safe, no flicker | no | implement in-house | build, debug preview, phone Matrix visual check | Keep visible as `READY`; hardware readability pass. |
| Battery | partial | `BatteryToyModule.kt` | Real battery level/charging/full/low visuals and automatic event overlay | Android battery broadcasts | implement in-house | build, phone logcat, manual plug/unplug check | Keep visible as `READY`; add status-event charging layer. |
| Compass | partial | `CompassToyModule.kt`, `SensorController.kt` | Rotation-vector heading, smoothing, calibration warning, readable circular gauge | Android sensors | implement in-house using Android rotation vector | sensor calibration test, phone logcat, Matrix visual check | Keep visible as `READY`; wire sensor accuracy/readiness display. |
| Text Scroll | partial | `TextScrollToyModule.kt`, `GlyphTextRenderer.kt` | Readable scrolling text with truthful settings | no | implement in-house | build, debug preview, manual Matrix text check | Keep visible as `READY`; physical readability validation. |
| PixelArt / Icon Viewer | partial | `PixelArtToyModule.kt`, `ToySettingsScreen.kt` | Named custom assets, import/export, direct Matrix test | Glyph Matrix Editor JSON, bitmap import | implement in-house with existing importers | build, debug preview, Matrix test | Keep visible as `READY`; add named asset repository UI. |
| Idle / Default Display | partial | `IdleDefaultToyModule.kt`, service default-display routing | Off, clock, battery, date, custom glyph, last active, selected toy; schedule-aware | no | implement in-house | phone logcat, AOD manual setup check | Keep hidden as host module; finish modes and schedule integration. |

## Editor / Content Creation

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Custom Matrix Icon Editor | partial | `ToySettingsScreen.kt`, `EditablePixelGrid` | Edit, save, rename, duplicate, delete, test on Matrix | no | implement in-house | build, debug preview, Matrix test | Add named custom asset store and controls. |
| Custom Animation Editor | missing | none | Multi-frame editor, duration, duplicate/delete, play preview, save canonical JSON | pixel editor formats already researched | implement smallest in-house version | build, debug preview | Add debug/advanced editor; do not expose as ready until persistence works. |
| Import/export assets | partial | `glyph.animation.*Importer` | Canonical JSON import/export, bitmap import, clean-room Glyph Matrix Editor import | Glyph Matrix Editor JSON, GIF/sprite optional | implement in-house, maybe Android bitmap APIs | build, import smoke | Add file/text import/export UI; keep Lottie unsupported until real dependency lands. |

## Sensor Tools

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Level | partial | `LevelToyModule.kt` | Calibrated bubble level, smoothing, haptics, lock state | Android accelerometer/filtering | implement in-house | sensor calibration test, Matrix visual check | Keep visible as `READY`; migrate calibration to shared repository. |
| Lux Meter | partial | `LuxMeterToyModule.kt` | Real light sensor, averaging, peak hold, circular gauge | Android light sensor | implement in-house | sensor test, phone logcat | Keep visible as `READY`; make gauge circular and add averaging. |
| Maze | hidden_experimental | `MazeToyModule.kt` | Tilt ball, walls, goal, reset, presets/generator | Android accelerometer/filtering | implement in-house | sensor test, Matrix visual check | Keep `EXPERIMENTAL`; add presets/reset/win before normal visibility. |
| Orientation / Angle Gauge | missing | none | Rotation-vector angle gauge with calibration | Android rotation vector | implement in-house | sensor calibration test | Add `OrientationToyModule` as `READY` if rotation vector exists, otherwise `NEEDS_SETUP`. |
| Step / Motion Toy | missing | none | Step detector/counter if available, motion fallback; no fake steps | Android step sensors | implement in-house | sensor permission/test | Add `MotionToyModule` as `NEEDS_SETUP` for activity recognition. |

## Audio Tools

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Tuner | hidden_experimental | `TunerToyModule.kt` | Microphone tuner, instrument presets, cents, smoothing, confidence rejection | YIN, TarsosDSP, AudioRecord | implement in-house YIN over AudioRecord | build, microphone test, phone logcat | Replace manual offset with live `PitchDetector`; mark `NEEDS_SETUP` until mic permission. |
| Sound Level Meter | missing | none | Microphone RMS/amplitude, approximate dB, peak hold | AudioRecord RMS | implement in-house | microphone test | Add `SoundMeterToyModule` as `NEEDS_SETUP`; label approximate. |
| Metronome | missing | none | BPM visual beat, optional haptic/sound, drift compensation | Android timing/audio/haptics | implement in-house | timing/haptic/sound test | Add `MetronomeToyModule` as `READY` with optional setup for sound/haptic. |

## Time / Planning

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Timer | partial | `TimerToyModule.kt` | Duration, pause/resume/reset, repeat, end haptic/sound/notification | Android vibrator/notifications | implement in-house | build, notification/haptic test | Keep visible as `READY`; add completion feedback controller. |
| Pomodoro / Focus | missing | none | Focus/break/long break cycles, ring, haptic/notification | timer scheduling | implement in-house | build, phone logcat | Add `PomodoroToyModule` as `READY`. |
| School Class Timer | hidden_experimental | `SchoolClassTimerToyModule.kt` | Timetable editor, subject labels, lesson/break detection, warnings | no | implement in-house | build, schedule calculation test | Keep `EXPERIMENTAL`; add JSON timetable setting and detection. |
| Schedule / Night Mode | missing | none | Custom rules limiting Matrix; exceptions; optional custom wake | AlarmManager limits | implement in-house | schedule test, phone logcat | Add shared schedule policy and docs; expose as app setting. |
| Alarm / Wake Animation | missing | none | Custom wake schedule, optional system next-alarm if accessible | AlarmManager, Settings next alarm | implement in-house | alarm/manual wake test | Add custom wake only; system next alarm documented future. |

## Fun / Interactive

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Eye | partial | `EyeToyModule.kt` | Lifelike eye states without fake camera/person tracking | no | implement in-house | debug preview, Matrix visual check | Keep visible as `READY`; hardware tuning. |
| Rock Paper Scissors | hidden_experimental | `RockPaperScissorsToyModule.kt` | Shake, stabilization wait, countdown, reveal, reset | accelerometer stability | implement in-house | sensor test, Matrix visual check | Keep `EXPERIMENTAL`; add countdown/stabilization. |
| Breath / Meditation | missing | none | Inhale/hold/exhale cycle with optional haptic | no | implement in-house | build, Matrix visual check | Add `BreathToyModule` as `READY`. |
| Sun / Moon Phase | missing | none | Date/time-based moon phase, sun/moon visuals | moon phase algorithm | implement in-house | build, date test | Add `SunMoonToyModule` as `READY`; no network required. |

## System / Status

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Charging Status Animation | partial | `BatteryToyModule.kt` | Automatic plug/unplug/full status event | battery broadcasts | implement in-house | phone plug/unplug logcat | Add `GlyphStatusEventRouter` and battery receiver. |
| Payment / Wallet / NFC Animation | missing | none | Manual visual trigger, optional NFC/Wallet-active; no payment success claim | NFC, UsageStats | implement in-house | manual trigger, NFC test | Add manual trigger and optional setup-gated monitors. |
| Volume Status Animation | missing | none | Short overlay on volume changes if reliable | audio manager volume receiver/callback | implement in-house | manual volume test | Add status event receiver where reliable; otherwise hide. |
| Notification Pulse | missing | none | Opt-in listener, app filtering, privacy-safe pulse | NotificationListenerService | implement in-house | permission flow/logcat | Add listener service as `NEEDS_SETUP`. |
| Network Status | missing | none | Wi-Fi/network connected status, simple signal indicator | ConnectivityManager | implement in-house | network test | Add `NetworkStatusToyModule` and status layer event. |
| Find Phone / Beacon | missing | none | Strong user-controlled beacon, optional sound/vibration | haptic/audio | implement in-house | manual start/stop test | Add `BeaconToyModule` with explicit timeout. |
| Alarm / Wake Animation | missing | none | Custom wake visual and morning summary | AlarmManager | implement in-house | manual schedule test | Add schedule-backed wake status event. |

## Web / Development

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Wi-Fi Web Portal / Dev Portal | missing | none | Local server, PIN/token, status/toys/activate/assets/test/log/settings routes | NanoHTTPD | integrate library behind adapter | web route test | Add NanoHTTPD dependency and disabled-by-default service/controller. |
| Web Matrix Editor | missing | none | Browser 13x13 editor, save/upload/test | web editor UX | implement in-house HTML/JS served by portal | browser route test | Add simple 13x13 editor route; no arbitrary code uploads. |

## Hub

| Name | Status | Current file(s) | Required final behavior | Research needed | Approach | Validation | Exact next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GlyphHub Main Toy | complete | `GlyphHubToyService.kt` | Single official service owns Matrix output | Nothing SDK docs | existing service | build, phone logcat, AOD check | Keep one service; add status router inside it. |
| 2x2 Widget Carousel | partial | widget package | Carousel, quick settings, start/stop, app routes | Android widgets | existing RemoteViews | phone tap test | Keep; tighten quick setting list and setup badges. |
| Quick Settings per Toy | partial | settings schema, widget package | Per-Toy practical controls only | no | implement in-house | widget test | Replace heuristic fallback with explicit scoped entries. |
| Full App Settings | partial | `AppSettingsScreen.kt` | Grouped app setup, permissions, schedule, portal, status layer | no | implement in-house | build, UI smoke | Add permissions/status/schedule/web sections. |
| Advanced / Debug Tools | partial | `DebugScreen.kt` | Asset lab, status events, logs, Matrix test | no | implement in-house | build, debug smoke | Add status event triggers and portal status. |

## Current Conclusion

The project has a stable service/render baseline and several useful Toys, but many requested tools are still missing or hidden. The next implementation must add real shared subsystems, setup-aware visibility, permission flows, and status-event routing before marking newly implemented privacy-sensitive tools as ready.

## 2026-06-07 Completion Pass Update

The following status changes were implemented after the initial audit and compiled with `:app:compileDebugKotlin`:

| Name | Updated status | File(s) | Visibility | Validation state | Remaining blocker / next action |
| --- | --- | --- | --- | --- | --- |
| Visibility model | complete | `ToyMetadata.kt`, `ToyRegistry.kt`, `ToyCard.kt`, `HomeScreen.kt` | READY/NEEDS_SETUP/EXPERIMENTAL/HIDDEN/MISSING enforced | build | Verify setup badges on phone UI. |
| Tuner | partial | `audio/PitchDetector.kt`, `audio/YinPitchDetector.kt`, `audio/AudioInputEngine.kt`, `audio/PitchTracker.kt`, `TunerToyModule.kt` | NEEDS_SETUP | build only | Microphone pitch reaction must be tested before claiming it works. |
| Sound Level Meter | partial | `audio/SoundLevelMeter.kt`, `SoundMeterToyModule.kt` | NEEDS_SETUP | build only | Microphone RMS/dB reaction must be tested; dB remains approximate. |
| Metronome | partial | `MetronomeToyModule.kt`, `AudioFeedbackController.kt`, `HapticFeedbackController.kt` | READY | build only | Manual timing/haptic/sound test needed. |
| Timer completion feedback | partial | `TimerToyModule.kt` | READY | build only | End haptic/sound needs physical validation; notification channel still TODO. |
| Pomodoro / Focus | partial | `PomodoroToyModule.kt` | READY | build only | Haptic/sound transitions and long-run drift need phone validation. |
| Orientation / Angle Gauge | partial | `OrientationToyModule.kt`, `SensorController.kt` | READY | build only | Rotation-vector calibration must be tested on hardware. |
| Step / Motion Toy | partial | `MotionToyModule.kt`, `SensorController.kt` | NEEDS_SETUP | build only | Activity Recognition permission and step sensor presence must be verified. |
| Breath / Meditation | partial | `BreathToyModule.kt` | READY | build only | Visual rhythm and haptic cues need physical Matrix validation. |
| Sun / Moon Phase | partial | `SunMoonToyModule.kt` | READY | build only | Date/phase output needs visual validation. |
| RPS | partial | `RockPaperScissorsToyModule.kt` | EXPERIMENTAL | build only | Countdown/stabilization implemented; shake/stable/reveal needs sensor and Matrix validation. |
| Maze | partial | `MazeToyModule.kt` | EXPERIMENTAL | previous sensor smoke only | Needs presets/generator and physical playability validation before normal visibility. |
| School Class Timer | partial | `SchoolClassTimerToyModule.kt` | EXPERIMENTAL | build only | Real timetable editor/detection is still missing. |
| Weather | partial | `weather/`, `WeatherToyModule.kt` | NEEDS_SETUP | build only | Open-Meteo fetch/cache/offline route must be tested on phone/network. |
| Schedule / Night Mode | partial | `schedule/SchedulePolicy.kt`, `AppSettings.kt`, `AppSettingsScreen.kt`, `GlyphHubToyService.kt` | app setting | build only | Needs time-window and exception tests; custom wake alarm still TODO. |
| System status layer | partial | `systemstatus/`, `GlyphHubToyService.kt` | service layer | build only | Charging/volume/notification/network/payment events need live logcat and Matrix tests. |
| Payment visual | partial | `PaymentToyModule.kt`, `GlyphStatusEventRouter.kt` | NEEDS_SETUP | build only | Manual visual exists; NFC and UsageStats wallet-active detection remain TODO. No payment success detection is claimed. |
| Network Status | partial | `NetworkStatusToyModule.kt` | READY | build only | Connectivity changes need phone validation. |
| Beacon | partial | `BeaconToyModule.kt` | READY | build only | Timeout/haptic/sound needs manual stop/timeout validation. |
| Notification Pulse | partial | `GlyphNotificationListenerService.kt` | NEEDS_SETUP service | build only | Permission/settings flow and app filters are not complete. |
| Web Portal / Web Matrix Editor | partial | `web/GlyphWebPortalManager.kt`, `AppSettingsScreen.kt` | disabled by default | build only | Token-gated routes compile; route testing over device/LAN still required. |

No newly added privacy-sensitive or externally dependent feature is marked fully complete until the matching microphone, network, notification, NFC, sensor, or portal validation is recorded in `PHONE_TEST_RESULTS.md`.
