# Architecture

GlyphHub exposes one Android service, `GlyphHubToyService`, registered with the official `com.nothing.glyph.TOY` action. Internally it routes to modular Toy modules.

## Layers

- App UI: Jetpack Compose screens under `ui/`. UI reads and writes settings, then requests service sync or activation.
- Widget: `widget/` renders a compact RemoteViews widget with one centered Toy preview bitmap and five explicit tap zones for previous, next, left settings, right settings, and center confirm/toggle.
- Toy service: `glyph/GlyphHubToyService.kt` loads the selected Toy, plays activation/deactivation transitions, runs the render loop, handles Nothing Toy bind/AOD `Messenger` events, and updates widgets.
- Glyph controller: `GlyphMatrixController` hides SDK details. `RealGlyphMatrixController` uses reflection over the official SDK classes (`GlyphMatrixManager`, `GlyphMatrixFrame.Builder`, `Common`, and `Glyph`) so the source still compiles without the AAR. `FakeGlyphMatrixController` keeps the app buildable and previewable when the AAR is absent.
- Matrix layout: `GlyphMatrixLayout` centralizes the 13x13 physical LED mask. Preview, editor, and SDK payload generation use this so custom artwork targets the circular physical matrix shape instead of an unrestricted square canvas.
- Toy modules: `toys/` contains `GlyphToyModule` implementations. Modules own settings schema, activation hooks, sensor hooks, and 13x13 frame rendering.
- Settings: `settings/` persists global and Toy-specific settings in SharedPreferences.
- Transitions: `GlyphTransitionEngine` generates reusable 13x13 transition animations. JSON transition assets are also present under `assets/glyphs/transitions/`.
- Sensors: `sensors/` gates accelerometer and rotation-vector input through `SensorController`. It runs only when global sensors are enabled and the active Toy supports sensors.

## Activation Flow

1. User taps the app or widget control.
2. `SettingsRepository` updates `selectedToyId` or `activeToyId`.
3. `GlyphHubToyService` receives an explicit request.
4. The service loads Toy settings, calls `onActivate`, plays the activation transition, starts the render loop, and starts sensors if allowed.
5. `GlyphHubWidgetRenderer.updateAll()` refreshes widget active state.

Selection-only widget/app browsing changes update `selectedToyId`, `lastSelectedToyIndex`, and `widgetCarouselIndex`, then request a service sync without replaying the activation transition for an already-active Toy.

When the Nothing system binds `GlyphHubToyService` as an official Toy, `onBind()` returns a `Messenger`. `EVENT_AOD` triggers a service sync so the configured active/default 13x13 display is rendered for Always-on Glyph Toy use. `EVENT_CHANGE` and action-down are currently mapped to the internal back-tap sensor event for modules that care about it.

## Deactivation Flow

1. The current Toy is toggled off.
2. The service plays the deactivation transition.
3. It clears `activeToyId`.
4. If AOD/default display is enabled, it renders the configured default display; otherwise it clears the Matrix.
5. Widgets are refreshed.

`defaultDisplayMode = off` clears the Matrix. `date` and `custom_glyph` use the Idle/Default Toy with a forced default-mode setting so they render even if the Idle Toy's own saved mode is different.

## Extending Toys

Add a new `GlyphToyModule` implementation, include it in `ToyRegistry.modules`, provide a settings schema, and generate 13x13 frames in `onTick`. Core UI, widget, and service logic do not need Toy-specific branches.

Custom artwork is handled as Toy settings, not a separate Toy branch. The Pixel Art Toy has a phone-side editor that writes encoded 13-row custom glyph data into its own settings and selects the `custom` glyph asset mode. Future import/export can reuse the same encoded-row contract or the JSON asset format.

## System Robustness

GlyphHub does not call the Nothing SDK from UI, widget receivers, or Toy modules. All Matrix access is routed through `GlyphHubToyService` and `GlyphMatrixController`, which reduces conflict with Nothing's own Glyph services and keeps foreground-service behavior explicit. Widget broadcasts request service sync through a foreground service path to satisfy Android background-start restrictions. Sensors are started only for the active Toy and only when global sensor support is enabled.

## Development Commands

Debug APKs accept Activity intent commands for ADB-driven testing from the app process:

```powershell
adb shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command activate --es toyId dice
adb shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command deactivate --es toyId dice
```

Supported commands are `activate`, `deactivate`, `clear`, `sync`, `test_activation`, `test_deactivation`, and `refresh_widget`. They are ignored in non-debuggable builds.
