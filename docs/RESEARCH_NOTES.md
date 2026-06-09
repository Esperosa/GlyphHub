# Research Notes

GlyphHub targets Nothing Phone (4a) Pro with a 13x13 Glyph Matrix and device identifier `Glyph.DEVICE_25111p`.

Official source used:

- https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
- https://developer.android.com/develop/ui/views/appwidgets
- https://developer.android.com/develop/ui/views/appwidgets/collections
- https://developer.android.com/reference/android/widget/RemoteViews
- https://developer.android.com/about/versions/14/changes/fgs-types-required

Community animation/editor sources reviewed on 2026-06-07:

- https://github.com/antonvidishchev/toyph and https://nothing.community/en/d/56750-toyph-an-open-source-glyph-toy-for-nothing-phone-4a-pro-just-in-time-for-may-the-4th
- https://github.com/anamite/GlyphMo and https://nothing.community/d/57502-glyph-mo-animated-glyph-icons-for-notifications
- https://github.com/alex-1121/glyph-matrix-lab and https://www.reddit.com/r/NothingTech/comments/1stf735/new_release_matrix_lab_a_sandbox_for_customizing/
- https://glyphmuseum.com/

Research conclusion:

- Toyph and GlyphMo show useful community patterns: JSON/frame-driven 13x13 assets, glitch/scanline-style effects, live preview, and importable custom frames. Their licenses were reviewed as MIT-compatible.
- GlyphHub now adapts generic MIT GlyphMo visual ideas through `CommunityGlyphVisuals.kt`, rewritten for `GlyphFrame`, 13x13-first rendering, and the circular physical LED mask. The adapted set intentionally excludes recognizable IP-specific arcade glyphs.
- GlyphHub now supports Toyph-style single-glyph JSON assets with `gridSize: 13` and a 169-value `brightness` array through `GlyphAssetLoader`. Toyph's demo Star Wars-style glyphs are not bundled as default app presets because those symbols are trademark/IP-adjacent despite the MIT project license.
- Glyph Matrix Lab is useful as product reference for custom image/editor workflows, but its GPLv3 license means its code/assets should not be copied into this project unless the project license strategy changes.
- Glyph Museum confirms the broader community direction: pixel-level Matrix editing, animation sharing, live effects, and web editor workflows. GlyphHub's current phone-side editor remains much smaller and is documented as partial.
- Third-party license decisions and notices are tracked in `docs/THIRD_PARTY_NOTICES.md`.

The official SDK AAR is installed at:

`app/libs/glyph-matrix-sdk-2.0.aar`

The AAR declares `minSdkVersion 33`, so the app module uses `minSdk = 33`. The target test device is Android 16, so this is compatible with the intended hardware.

SDK findings:

- Phone (4a) Pro is the SDK symbol `Glyph.DEVICE_25111p`.
- The SDK field value for that symbol resolves to `A069P`; `RealGlyphMatrixController` reads the field reflectively before calling `register(...)`.
- The runtime matrix size is detected with `Common.getDeviceMatrixLength()` and falls back to the SDK `DEVICE_25111p_MATRIX_LENGTH` field.
- Real rendering uses `GlyphMatrixFrame.Builder.addTop(int[])`, `build(Context)`, and `GlyphMatrixManager.setMatrixFrame(GlyphMatrixFrame)`.
- The raw 25-size-oriented overload is not used by GlyphHub for 13x13 rendering.
- `GlyphHubToyService` now returns a `Messenger` from `onBind()` and handles `GlyphToy.EVENT_AOD` for system Always-on Glyph Toy refresh.

The project still compiles without the AAR. SDK integration is isolated behind `GlyphMatrixController`; when SDK classes are absent, the app uses `FakeGlyphMatrixController`. This keeps Compose previews, widget rendering, Toy logic, and asset parsing buildable without proprietary SDK classes.

Logcat validation on the connected target (`A069P` / `FroggerPro`) confirmed:

- `service created sdkMode=real`
- `Real Glyph SDK bridge initialized device=Glyph.DEVICE_25111p sdkTarget=A069P matrix=13x13`
- `Real Glyph SDK service connected registered=true`
- Activation/deactivation through the app process updates the widget and renders default display after deactivation.
- The latest smoke pass activated/deactivated `dice` and the new manual `tuner` Toy without a fatal exception, and default-display fallback selected `clock` after deactivation when configured.
- `assembleDebug` was also verified with `app/libs/glyph-matrix-sdk-2.0.aar` temporarily removed and restored afterward, confirming the fake/stub build path remains valid.
- The 2026-06-07 sensor smoke activated `compass`, `level`, `lux_meter`, `dice`, `coin`, and hidden `maze`; logcat showed sensor availability and first events from accelerometer, light, magnetic field, and rotation vector.
- Default display was retested with `aodEnabled=true` and `defaultDisplayMode=battery`; service sync logged `default_display` and `default display module=battery mode=battery`.

Local testing does not require Nothing Playground. Playground/community packaging is a later publishing step after real-device validation.

Widget and service findings:

- Android widgets are `RemoteViews`; launcher hosts support only a constrained set of view operations and commonly intercept swipes for workspace navigation.
- Nothing Launcher reliably handled explicit `RemoteViews` tap zones, while swipe-style widget navigation stayed inconsistent. GlyphHub now uses one centered preview bitmap plus five explicit touch regions instead of stacked collection views.
- Nothing Launcher rejected a widget implementation that used invisible plain `android.view.View` hit targets, so GlyphHub uses invisible `TextView` zones.
- The requested widget settings overlay is implemented as an inline `RemoteViews` panel because widgets cannot create arbitrary launcher-level overlays, capture Back, or detect taps outside their own bounds.
- Widget-triggered Matrix sync starts the renderer as a foreground service with `specialUse` type so Android background service launch limits do not block rendering from a broadcast receiver.

Conflict analysis:

- Matrix rendering is isolated to `GlyphHubToyService` and `GlyphMatrixController`; UI, widget, and Toy modules never talk to the Nothing SDK directly.
- The app uses one official `com.nothing.glyph.TOY` service, so Nothing Settings sees one Toy entry while the app manages internal modes.
- Widget-triggered rendering uses a foreground service path to avoid Android background-start failures from `BroadcastReceiver`.
- Sensor collection is gated by global settings and active Toy capability, reducing unnecessary sensor use while other system features are running.
- GlyphHub should avoid presenting quick controls as system-level Glyph brightness controls. Nothing OS remains the final authority for device brightness; GlyphHub's remaining app-side control is treated as relative frame intensity.
