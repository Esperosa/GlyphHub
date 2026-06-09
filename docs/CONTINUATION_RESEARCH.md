# Continuation Research

## Scope

This document now serves mainly as the pre-expansion research baseline for the continuation pass. For the current post-expansion repo state, see `docs/IMPLEMENTATION_STATUS.md`, `docs/PHONE_TEST_RESULTS.md`, and `docs/ADDITIONAL_TOYS.md`.

This document records the current verified state of GlyphHub before the next continuation pass. It combines local code audit findings, real-device runtime validation, and official Android/Nothing reference material.

## Verified Baseline

- Target repo: `d:\ToyHub3aPro`
- Runtime target used during validation: Nothing `A069P` / `FroggerPro` on Android 16
- Official SDK artifact in use: `app/libs/glyph-matrix-sdk-2.0.aar`
- Current internal Toy registry count: 8 modules
- Current widget model: one centered preview bitmap plus five explicit tap zones inside a 2x2 RemoteViews widget

## Official Reference Findings

### Nothing Glyph Matrix Developer Kit

Source references used:

- Local mirror: `artifacts/research/GlyphMatrix-Developer-Kit/README.md`
- Upstream: `https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit`

Key findings:

- Phone (4a) Pro uses `Glyph.DEVICE_25111p` and a 13x13 matrix.
- Phone (4a) Pro does not support Glyph Touch and only exposes AOD Toy capability.
- A Glyph Toy is expected to start work from `onBind()` and stop cleanly from `onUnbind()`.
- `EVENT_AOD` is the relevant system callback for AOD Toy refresh on Phone (4a) Pro.
- The SDK supports `GlyphMatrixManager.setMatrixFrame(GlyphMatrixFrame)` for Toy rendering.
- The SDK documentation explicitly notes that Toy content has higher priority than direct app matrix usage.

Implication for GlyphHub:

- Keeping all real Matrix writes inside the official Toy service remains the correct architecture for this device class.
- App-side direct matrix control should not be added as the primary runtime path for active Toy behavior.

### Android Widget and RemoteViews Guidance

Source references used:

- `https://developer.android.com/develop/ui/views/appwidgets`
- `https://developer.android.com/reference/android/widget/RemoteViews`

Key findings:

- Widgets are rendered through `RemoteViews` and cannot use arbitrary custom views.
- Only supported platform layouts/widgets should be used; subclasses/custom views are not supported.
- Widget state is still app-owned; the host can reapply or recreate the RemoteViews at any time.
- Collection widgets remain possible, but `RemoteViews` collection behavior is host-dependent and `showNext()` / `showPrevious()` are deprecated in newer API guidance.

Implication for GlyphHub:

- The current single-preview widget is more robust on Nothing Launcher than the older StackView path.
- Explicit tap targets remain the reliable interaction model for this launcher/device combination.

### Android 14 Foreground Service Guidance

Source reference used:

- `https://developer.android.com/about/versions/14/changes/fgs-types-required`

Key findings:

- Android 14 requires an appropriate foreground service type to be declared.
- `specialUse` is valid for foreground service cases not covered by the standard categories.
- Manifest declarations and runtime `startForeground()` behavior must stay aligned.

Implication for GlyphHub:

- The widget-triggered renderer sync path should continue to use the explicit foreground service route.
- Any future manifest cleanup must preserve the `specialUse` declaration and associated permission/property wiring.

## Current Architecture Snapshot

- `GlyphHubToyService` owns activation, deactivation, default display, transition playback, render loop, and widget refresh.
- `GlyphMatrixController` isolates SDK access behind `RealGlyphMatrixController` or `FakeGlyphMatrixController`.
- `SensorController` starts accelerometer and rotation-vector input only when the active Toy supports sensors and global sensors are enabled.
- `GlyphHubWidgetRenderer` renders a single centered preview and binds top, bottom, left, right, and center actions.
- `SettingsRepository` persists both app-level settings and per-Toy settings through SharedPreferences.

## Current Toy Inventory

### Dice

- Visual: circular ring with centered pips or numeric fallback for more than six sides
- Runtime behavior: rolling animation works, brightness pulses, sensor-triggered usage confirmed
- Current gap: phone-back validation still needs to confirm roll cadence and settle pulse readability

### Coin

- Visual: coin circle with center label
- Runtime behavior: functional flip animation works
- Current gap: settings do not yet expose richer visual design sets or stronger result stabilization

### Compass

- Visual modes: arrow, cardinal text, minimal dot
- Runtime behavior: rotation-vector path confirmed on the target phone
- Current gap: smoothing option is defined but not materially implemented in render behavior

### Clock

- Visual modes: digits, abstract, binary, dot clock
- Runtime behavior: active/default display path works
- Current gap: centering and flicker resilience need work during repeated refresh/render ownership changes

### Battery

- Visual: battery outline with fill rows
- Runtime behavior: functional default display module
- Current gap: `showPercentage` setting is not reflected in current output, charging-specific behavior is limited

### Pixel Art

- Visuals: built-in assets plus custom rows from the in-app editor
- Runtime behavior: custom editor works, selected asset renders
- Current gap: `animationMode` and timing settings are largely no-ops in current output

### Text Scroll

- Visual: scrolling 3x5 text renderer
- Runtime behavior: functional scrolling renderer
- Current gap: font variants and some loop semantics are incomplete or misleading versus the exposed settings

### Idle Default

- Role: default-display host module for clock/date/custom glyph style output
- Runtime behavior: used for app-level default display fallback
- Current gap: several settings around selected default toy / animation behavior are not fully connected to rendering

## Real Device Findings

Verified from build/install/runtime work already performed:

- Real SDK mode was confirmed through stored debug logs: `sdkMode=real`.
- Real bridge initialization was confirmed with 13x13 matrix detection.
- Official Toy bind/AOD path is live through `Messenger` handling in the service.
- Compass activation confirmed rotation-vector sensor events on the real target.
- All current Toys were activated successfully at runtime from the debug command path.
- The widget overlap bug was removed by replacing the stacked widget preview approach with a single centered preview bitmap.

Still pending on physical hardware:

- Direct visual confirmation of LED orientation/brightness on the phone back for multiple Toys and transitions.
- Manual validation of GlyphHub selection and visibility in Nothing's Always-on Glyph Toy settings.
- Cross-launcher validation of the current widget tap zone layout.

## Code Audit Findings

### Highest-Risk Reliability Problems

1. `GlyphHubToyService` currently mixes lifecycle transitions, default-display fallback, loop ownership, and direct controller writes in one class without an explicit state machine.
2. `playTransition()` blocks the render handler thread with `SystemClock.sleep(...)`, which prevents clean interleaving of transition playback, AOD syncs, widget-triggered syncs, and deactivate/reactivate sequences.
3. Render ownership is implicit instead of explicit. The loop, transition playback, `clear()`, and default display can all become the effective writer without a single session token or scheduler.
4. `autoReturnToDefault` and `autoReturnDelaySeconds` are persisted settings but not implemented as runtime behavior.
5. Sensor delivery is not serialized onto the same execution context as service render ticks, so Toy state can be mutated concurrently.

### Truthfulness Gaps Between Settings and Runtime

- `Compass.smoothRotation` is effectively a no-op.
- `PixelArt.animationMode` and timing-related fields are not meaningfully honored.
- `TextScroll.font` exposes variants that are not fully implemented.
- `Battery.showPercentage` is not reflected in output.
- `activationAnimationOverride` / `deactivationAnimationOverride` accept invalid strings without UI validation.
- `BackTapDetector` remains hard-disabled in `SensorController`.

### Documentation / Backlog Drift Before This Pass

- `docs/TODO.md` still reflected the older project phase and did not focus on the actual reliability bottleneck.
- `todos.json` still referenced deleted StackView widget work as if it were current.
- `docs/ARCHITECTURE.md` was already closer to the current codebase than the backlog files.

## Recommended Priority Order

### Priority 1: Service and Render Ownership

- Add an explicit service lifecycle state model.
- Move transition playback to non-blocking scheduled execution on the render handler.
- Introduce render-session ownership so stale transitions, loops, and clears cannot overwrite the current state.
- Implement auto-return-to-default with proper cancellation rules.
- Guard render ticks and log lifecycle/state changes.
- Serialize sensor delivery or otherwise make Toy state mutation single-owner.

### Priority 1: User-Visible Toy Corrections

- Dice: make `sides > 6` truthful, not just a wrapped six-pip visual.
- Coin: add more stable flip/result presentation and design variants.
- Compass: implement real smoothing and reduce jitter.
- Battery: use the exposed percentage setting and improve charging/default behavior.
- Clock: reduce flicker and improve centering consistency.

### Priority 2: Settings/UI Truthfulness

- Validate transition override ids in the UI.
- Remove or implement dead settings.
- Align widget/app settings controls with actual runtime behavior.

### Priority 3: Coverage and Publishing

- Add tests around service lifecycle and settings-driven rendering.
- Finish physical validation and secondary-launcher widget checks.
- Prepare publishing assets only after the reliability pass is complete.

## Continuation Goal for the Next Edit Pass

The next implementation pass should start in `app/src/main/java/com/pelikan/glyphhub/glyph/GlyphHubToyService.kt` and establish one authoritative owner for the Matrix output path. The service should stop acting as a loose collection of imperative lifecycle branches and instead schedule render work through explicit session/state control.
