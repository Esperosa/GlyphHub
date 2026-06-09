# TODO

This backlog reflects the fresh 2026-06-06 audit/build/device baseline, not the older first-pass continuation snapshot.

## Verified Baseline

- [x] `:app:compileDebugKotlin` passes.
- [x] `assembleDebug` passes.
- [x] `todos.json` parses cleanly.
- [x] Debug APK installs on Nothing `A069P` / `FroggerPro` (`003203627001082`).
- [x] `MainActivity` launches without an immediate crash in filtered logcat.
- [x] Official Nothing Glyph Matrix integration, fake fallback, and the one-service architecture remain intact.
- [x] The launcher widget still uses one centered preview plus five explicit tap zones instead of the removed stacked-preview path.
- [x] `ToyRegistry` now exposes 16 modules instead of the previous 8.
- [x] App and launcher widget previews render compact LED-only 13x13 visuals on the connected phone after the 2026-06-07 preview-size fix.
- [x] `docs/TOY_COMPLETENESS_AUDIT.md` separates live, partial, missing, and host-feature items from the expanded requested Toy list.
- [x] 2026-06-07 animation/community research completed; Toyph/GlyphMo/Glyph Matrix Lab/Glyph Museum were reviewed and license constraints recorded in `docs/RESEARCH_NOTES.md`.
- [x] Generic MIT community visuals were adapted into `CommunityGlyphVisuals.kt`, Toyph brightness-grid import support was added, and third-party notices were documented.
- [x] PixelArt now has a shared built-in icon library, custom editor save path, and widget/app preview path for custom rows.
- [x] Sensor smoke on `A069P` confirmed event flow for Compass, Level, LuxMeter, Dice, Coin, and hidden Maze with sensors enabled.
- [x] Default display smoke confirmed `defaultDisplayMode=battery` renders through the service when no Toy is active and AOD is enabled.
- [x] `docs/ANIMATION_TOOLING_RESEARCH.md` records official Nothing, Android, Lottie, pixel-editor, community Glyph, pitch/sensor/weather, and web-portal research.
- [x] `docs/ANIMATION_PIPELINE_DECISION.md` selects an internal 13x13 canonical asset/frame pipeline with optional external importers.
- [x] Shared `glyph.assets`, `glyph.animation`, and `glyph.validation` packages now exist and compile.
- [x] Service output now runs through central frame validation/normalization before controller dispatch.

## Priority 1: Complete the Missing Toy Surface

- [x] Add and register Level, Timer, Eye, Weather, Maze, RPS, LuxMeter, and SchoolClassTimer.
- [x] Add the remaining requested core Toy: Tuner.
- [ ] Add a small additional Toy set only after the requested core list exists and compiles cleanly.

## Priority 1: Make Existing Toys Truthful

- [ ] Coin: tune the new requested result-set implementation on hardware and improve the iconography beyond centered labels.
- [ ] Compass: add richer direction modes and make calibration-related behavior truthful.
- [x] Clock: add analog mode and move the main clock visuals to circular-safe composition.
- [x] Battery: replace rectangular battery imagery with circular gauge composition.
- [x] Pixel Art: make animation-related settings actually affect output and route built-in/custom previews through one icon renderer.
- [x] Pixel Art: stop shifting static icons for animation modes and add centered/full-size heart plus generic animated community presets.
- [x] Text Scroll: implement real font variants and truthful loop behavior.
- [x] Eye: replace the fixed loop with stateful weighted behavior.
- [x] Level: add center target, calibration, smoothing, and haptic guidance.
- [x] Dice: add debounce and a settle phase instead of abrupt finish.
- [x] Idle / default display: connect and smoke-test the `battery` default display path through real service sync.
- [ ] Idle / default display: finish selected Toy, custom glyph, weather, and last-active behavior.

## Priority 1: Finish the Editor and Settings UX

- [x] Replace free-text transition override entry with validated transition choices.
- [x] Remove brightness from widget quick-setting surfaces and rename the app-facing global control to Matrix intensity.
- [x] Hide the app-level Matrix intensity slider outside debug mode so normal UI leaves global Glyph brightness to Nothing OS.
- [x] Add stable per-Toy preview frames so widget/app cards do not call runtime `onTick(0L)` for icons.
- [x] Replace empty/oversized app preview rendering with fixed-size Compose LED previews and explicit 13x13 widget/app icon patterns.
- [x] Replace misleading circular Dice/Battery preview patterns with object-specific 13x13 previews.
- [x] Replace the too-small Dice visual with a larger square shell and single-LED pips shared by runtime and preview.
- [ ] Rename remaining per-Toy `brightness` schema keys to visual-intensity semantics without breaking saved settings.
- [x] Expand the pixel/icon editor beyond the original custom-rows toggle with built-in icon selection plus clear/fill/invert/mirror/rotate/shift actions.
- [ ] Add save/rename/duplicate/delete for multiple named custom PixelArt assets.
- [ ] Reconcile widget/app settings controls with the actual service sync path after each change.

## Priority 1: Render Quality Hardening

- [x] Replace square/all-on generic transitions with circular-safe transition patterns.
- [x] Replace noisy random fade transition frames with stable circular fade/ring frames.
- [x] Preserve per-pixel intensity during overlay and shift composition.
- [x] Route final service frames through `GlyphRenderScheduler` no-flash rejection/logging.
- [x] Route final service frames through `GlyphFrameValidator` for 13x13, circular-mask, all-on/all-off, stray-pixel, centering, and intensity checks.
- [x] Add a debug Asset Quality / Animation Lab that lists canonical assets and Toy preview outputs with validator metrics.
- [x] Move Dice, Coin, Battery gauge, and Compass arrow output onto shared icon primitives.
- [x] Remove hard brightness-pulse/flicker from Dice, Coin, Battery, Timer, RPS, Maze, School Timer, Weather, and PixelArt default rendering.
- [x] Expand generic transitions and common circular gauges to use the full 13x13 physical ring radius.
- [ ] Push the remaining placeholder/partial Toys through the circular design system and stray-pixel cleanup.
- [ ] Implement full import/export UI for Bitmap/GIF/sprite/Glyph Matrix Editor JSON assets.
- [ ] Add real Lottie Android integration only after dependency/performance review; current `LottieGlyphImporter` is an honest unsupported stub.

## Priority 2: Widget and Phone Validation

- [x] Re-run widget touch-zone and scaling checks on the current launcher after the fresh install for center, top, bottom, and left.
- [x] Fix widget renderer so Toy quick-panel state is drawn inside the widget instead of opening the app.
- [x] Fix launcher hit-target precedence by replacing the overlapping `FrameLayout` zones with non-overlapping `TextView` zones.
- [x] Validate the right widget zone physically from the exact widget page; it opens full app settings.
- [x] Validate widget Toy-panel left-zone opening on the launcher after the final mapping change.
- [x] Validate widget right-zone opening of the full app from the exact launcher page.
- [ ] Validate widget Toy-panel `+/-` adjustment taps on the launcher after the final mapping change.
- [ ] Physically verify phone-back LED visual quality for active Toys, transitions, and default display by direct observation, not only logcat.
- [ ] Validate Nothing AOD / Always-on Glyph Toy integration from system settings.
- [ ] Expand `docs/PHONE_TEST_RESULTS.md` and add a dedicated widget result document.

## Priority 3: UI Polish, Tests, and Release Readiness

- [ ] Continue improving the main UI so the expanded Toy inventory stays usable and the controls remain truthful; the 2026-06-07 pass fixed blank previews, oversized previews, and wrapped selected-state labels.
- [ ] Add focused tests around service lifecycle, settings-driven rendering, and default-display fallback.
- [ ] Add asset import/export only if the editor becomes broad enough to justify it.
- [ ] Prepare release/publishing assets only after the runtime and Toy surface are materially complete.

## 2026-06-07 App Completion Pass TODOs

- [x] Create `docs/APP_COMPLETION_AUDIT.md` before feature coding.
- [x] Create `docs/APP_LIBRARY_RESEARCH.md` before feature coding.
- [x] Replace `Visible/HiddenExperimental` with READY/NEEDS_SETUP/EXPERIMENTAL/HIDDEN/MISSING visibility states.
- [x] Add contextual microphone and activity-recognition permission prompts from Toy settings.
- [x] Add in-house YIN-style pitch detection behind `PitchDetector`; do not integrate GPL TarsosDSP.
- [x] Add AudioRecord-backed approximate Sound Level Meter.
- [x] Add drift-compensated Metronome.
- [x] Add Pomodoro, Orientation, Step/Motion, Breath, Sun/Moon, Network Status, Payment Visual, and Beacon modules.
- [x] Convert Weather to Open-Meteo with cached fallback.
- [x] Add custom schedule/night policy and app settings.
- [x] Add `GlyphStatusEventRouter` and opt-in notification listener/status receiver scaffolding.
- [x] Add NanoHTTPD web portal route skeleton with token-gated status/toy/asset/test/settings/log routes.
- [ ] Live-test Tuner microphone pitch reaction, cents stability, and instrument presets.
- [ ] Live-test Sound Level Meter microphone RMS/peak response.
- [ ] Live-test Metronome drift, haptic tick, and optional sound tick.
- [ ] Live-test Timer and Pomodoro completion feedback.
- [ ] Live-test Open-Meteo fetch, cache, and offline fallback.
- [ ] Live-test web portal routes over LAN with token.
- [ ] Live-test NotificationListenerService permission flow and app filtering before surfacing Notification Pulse as ready.
- [ ] Implement NFC and UsageStats wallet-active triggers only if they can be labelled truthfully; manual payment visual is the only compiled payment path now.
- [ ] Implement custom wake/alarm scheduler; system next-alarm integration is not done.
- [ ] Add named PixelArt asset save/rename/duplicate/delete and multi-frame animation editor persistence.
- [ ] Add phone-back Matrix visual validation for every new READY/NEEDS_SETUP/EXPERIMENTAL module before calling it complete.
