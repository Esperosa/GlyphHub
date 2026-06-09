# Toy Completeness Audit

Status captured on 2026-06-07 after the app/widget preview polish, PixelArt editor, animation-radius, and sensor smoke pass.

Legend:

- `LIVE`: registered module exists and can be launched through GlyphHub.
- `PARTIAL`: registered module exists, but important requested behavior is still incomplete.
- `MISSING`: not implemented as a GlyphHub Toy/module yet.
- `HOST FEATURE`: part of the hub/editor/settings surface rather than a standalone Toy.

## Core Apps

| Requested app | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| Dice | PARTIAL | Registered as `dice`; supports configurable sides, shake roll, debounce, settle hold, and a square dice shell for runtime/preview. | Physical Matrix tuning for all polyhedral sides and better high-side numeric layouts. |
| Coin | PARTIAL | Registered as `coin`; supports result sets and shake flip. Preview is coin-specific. | Replace remaining centered-label result frames with clearer glyph outcomes where possible. |
| Clock | PARTIAL | Registered as `clock`; supports digits, analog, abstract, binary, dot clock, AOD/default display path. | Real phone readability validation; colon blink defaults off to avoid flicker. |
| Battery | PARTIAL | Registered as `battery`; reads BatteryManager and shows level/charging/low state. Preview is now battery-shaped. | Stronger full/charging/disconnect event animations. |
| Compass | PARTIAL | Registered as `compass`; sensor-backed heading and multiple display modes exist. 2026-06-07 smoke confirmed accelerometer/magnetic/rotation/light sensor event flow starts on device. | Richer calibration confidence UI and jitter tuning. |
| Text Scroll | PARTIAL | Registered as `text_scroll`; has text, direction, speed, font, loop settings. | Real Matrix readability and app-side editing polish. |
| PixelArt / Icon Viewer | PARTIAL | Registered as `pixel_art`; custom rows, built-in icon library, custom editor, and app/widget custom preview path exist. Default static preview no longer pulses. | Multiple named custom assets, import/export, and richer custom animations. |
| Idle / Default Display | PARTIAL | Registered as `idle_default`; service has default display routing. `battery` default mode was smoke-tested with AOD enabled. | Finish selected Toy / last active / weather / custom glyph behavior and system AOD selection validation. |

## Editor / Content Creation

| Requested feature | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| Custom Matrix Icon Editor | PARTIAL | 13x13 editor exists for PixelArt custom rows and now supports clear, fill, invert, mirror, rotate, shift, immediate save, and shared app/widget preview. | Brush/eraser modes, save/rename/duplicate/delete for multiple custom assets, and direct Matrix test button. |
| Custom Animation Editor | MISSING | No multi-frame editor yet. | Frame list, duration, duplicate/delete, preview animation, effects. |

## Sensor Tools

| Requested app | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| Level / Vodováha | PARTIAL | Registered as `level`; accelerometer-backed target/bubble and calibration exist. 2026-06-07 smoke confirmed sensor startup/event flow. | Physical calibration and haptic tuning. |
| Lux Meter | PARTIAL | Registered as `lux_meter`; light sensor path exists. 2026-06-07 smoke confirmed the light sensor is available and events are delivered. | More polished lux units, averaging, and visual modes. |
| Maze | PARTIAL | Registered as hidden `maze`; accelerometer tilt movement exists. 2026-06-07 smoke confirmed sensor startup/event flow. | Circular/cropped maze design, win-state polish, generation/reset UI. |
| Orientation / Angle Gauge | MISSING | No dedicated module. | Add calibrated angle display/gauge module. |
| Step / Motion Toy | MISSING | No dedicated module. | Add motion/step sensor module. |

## Audio Tools

| Requested app | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| Tuner | PARTIAL | Live microphone-backed `TunerToyModule` exists and is setup-gated. | Real microphone validation, cents stability, presets. |
| Sound Level Meter | PARTIAL | `SoundMeterToyModule` exists with approximate dB, peak hold, and setup-gated microphone permission. | Real microphone validation and calibration language. |
| Metronome | PARTIAL | `MetronomeToyModule` exists with visual timing. | Drift, haptic/tone behavior, and phone-back validation. |

## Time / Planning

| Requested app | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| Timer | PARTIAL | Registered as `timer`; countdown, pause/reset, progress + digits exist. End state no longer blinks. | Notification/sound/vibration finish behavior and better setup UI. |
| Pomodoro / Focus | PARTIAL | `PomodoroToyModule` exists with focus/break cycle visuals. | Completion feedback and notification behavior. |
| School Class Timer | PARTIAL | Registered as hidden `school_class_timer`; class/break cycle timer exists. | Actual timetable presets, subject labels, warning patterns. |
| Schedule / Night Mode | MISSING | No dedicated module. | System-safe Matrix schedule, allowed exceptions. |

## Fun / Interactive

| Requested app | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| Eye | PARTIAL | Registered as `eye`; weighted behavior states and several styles exist. | Physical tuning and optional tracking only if implemented honestly. |
| Rock Paper Scissors | PARTIAL | Registered as hidden `rps`; shake/tap result exists. Animation no longer brightness-pulses. | Countdown 3-2-1 and stop-when-phone-stops logic. |
| Breath / Meditation | PARTIAL | `BreathToyModule` exists with breathing visual. | Physical smoothness validation. |
| Sun / Moon Phase | PARTIAL | `SunMoonToyModule` exists. | Date accuracy and readability validation. |

## System / Status Layers

| Requested feature | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| Charging Status Animation | PARTIAL | Battery Toy can show charging state. | Automatic plug/unplug event layer and full-charge animation. |
| Payment / Wallet / NFC Animation | PARTIAL | Manual `PaymentToyModule` and status event visuals exist; NFC/UsageStats are not requested or implemented. | Safe trigger UX only; no transaction-success claims. |
| Volume Status Animation | PARTIAL | Status event rendering path exists. | Manifest/dynamic broadcast validation and visual tuning. |
| Notification Pulse | PARTIAL | Notification listener service scaffold exists. | User opt-in flow, filters, and live callbacks. |
| Network Status | PARTIAL | `NetworkStatusToyModule` exists. | Real connectivity/signal validation. |
| Find Phone / Beacon | PARTIAL | `BeaconToyModule` exists. | Trigger UX and strong visual validation. |
| Alarm / Wake Animation | MISSING | No module/layer. | Alarm/schedule integration. |

## Web / Development Tools

| Requested feature | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| Wi-Fi Web Portal / Dev Portal | MISSING | No local web server. | Pairing token, upload/edit/test/log/settings endpoints. |
| Web Matrix Editor | MISSING | No web editor. | Browser 13x13 editor and live phone test path. |

## Hub Features

| Requested feature | Current status | Current implementation | Missing work |
| --- | --- | --- | --- |
| GlyphHub Main Toy | LIVE | Single official `GlyphHubToyService` registered with `com.nothing.glyph.TOY`. | Real AOD/system settings validation still required. |
| 2x2 Widget Carousel | LIVE | Five tappable zones, centered LED preview, active red styling, quick panel/full app routes. 2026-06-07 UIAutomator/tap smoke confirmed left = quick panel and right = app. | True swipe remains launcher-limited; tactile/visual polish can continue. |
| Quick Settings per Toy | PARTIAL | Widget inline panel exists for quick controls; PixelArt quick panel can show/cycle the selected glyph asset including `custom`. | Validate every `+/-` adjustment and tighten per-Toy quick setting whitelist. |
| Full App Settings | PARTIAL | Dynamic settings screen exists. | More user-friendly grouping and wording. |
| Advanced / Debug Tools | PARTIAL | Debug screen with SDK/matrix/tests/logs exists. | Add stronger render/session state inspection. |

## Current Truth

The project does not yet contain every app from the full requested list. It currently has the main hub, the widget carousel, and 17 registered Toy modules. Existing visible Toys now have non-black app/widget preview output, PixelArt custom artwork persists and renders in widget/app previews, and core sensor-backed modules start real Android sensors on the connected phone. Several modules are still partial and many proposed system/audio/web tools are not implemented yet.
