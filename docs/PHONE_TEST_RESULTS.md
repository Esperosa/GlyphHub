# Phone Test Results

Baseline captured on 2026-06-06 against the currently connected authorized device.

## Device inventory

- Authorized target: Nothing Phone (4a) Pro
- Manufacturer: `Nothing`
- Model: `A069P`
- Device codename: `FroggerPro`
- Android version: `16`

## Commands executed

1. `./gradlew.bat :app:compileDebugKotlin`
2. `./gradlew.bat assembleDebug`
3. `Get-Content ./todos.json -Raw | ConvertFrom-Json | Out-Null`
4. `pwsh -NoProfile -ExecutionPolicy Bypass -File ./scripts/device-check.ps1 -Serial <serial>`
5. `pwsh -NoProfile -ExecutionPolicy Bypass -File ./scripts/install-debug.ps1 -Serial <serial>`
6. `pwsh -NoProfile -ExecutionPolicy Bypass -File ./scripts/logcat-glyphhub.ps1 -Serial <serial>`
7. `pwsh -NoProfile -ExecutionPolicy Bypass -File ./scripts/launch-app.ps1 -Serial <serial>`
8. `adb -s <serial> shell monkey -p com.pelikan.glyphhub 1`

## Verified results

- `:app:compileDebugKotlin`: passed
- `assembleDebug`: passed
- `todos.json` parse validation: passed
- Debug APK install: passed (`adb install -r` returned `Success`)
- Activity launch: passed; `MainActivity` was already the top-most instance, so the explicit start was delivered to the existing task
- Package-manager launch (`monkey -p com.pelikan.glyphhub 1`): passed
- Filtered logcat during launch: no `FATAL EXCEPTION` and no immediate GlyphHub crash signature observed

## Retest after Toy expansion and settings-truthfulness changes

Additional commands executed after the larger code change set:

1. `./gradlew.bat :app:compileDebugKotlin` after the first new-Toy batch
2. `./gradlew.bat :app:compileDebugKotlin` after the second new-Toy batch
3. `./gradlew.bat :app:compileDebugKotlin` after the Pixel Art / Text Scroll / transition-UI truthfulness patch
4. `pwsh -NoProfile -ExecutionPolicy Bypass -File ./scripts/smoke-test-device.ps1 -Serial <serial>`
5. fresh filtered `logcat-glyphhub` capture during the smoke-script install and launch

Retest results:

- Kotlin compile passed after every code slice in this pass
- The repo smoke script rebuilt, reinstalled, and relaunched the updated APK successfully
- Filtered post-change logcat did not show `FATAL EXCEPTION`, `AndroidRuntime`, `tick failed`, or `glyph event failed`
- Logcat showed the Nothing `GlyphToyController` refreshing the registered `GlyphHubToyService` entry after package replacement
- The package-replace flow force-stopped the old process during install, which is expected for `adb install -r`

## Key log observations

- `ProfileInstaller` ran for `com.pelikan.glyphhub`
- `ActivityTaskManager` reported `com.pelikan.glyphhub/.MainActivity` launch/restart events with result code `3` (`LAUNCH_SINGLE_TOP` into the existing top activity)
- No crash trace, uncaught exception, or `AndroidRuntime` fatal termination from GlyphHub was observed in the captured baseline window
- In the later smoke rerun, `ActivityTaskManager` launched a fresh `MainActivity` instance after the package update with result code `0`
- `GlyphToyController` logged the updated `GlyphHubToyService` registration after the install

## Not yet verified in this pass

- Visual confirmation of the phone-back LEDs for Dice, Coin, Clock, Battery, transitions, and default display
- Nothing Settings integration path for Always-on Glyph Toy selection and AOD behavior
- Final right-zone widget tap validation from the exact launcher page after the latest mapping change
- Extended runtime soak for service/session stability

## Required manual visual validation checklist for the current pass

- Confirm no full-matrix flash during activation/deactivation
- Confirm no whole-panel flash during steady Toy updates
- Confirm Dice shake does not glitch
- Confirm Coin flip does not glitch
- Confirm Clock is centered and free of stray pixels
- Confirm Battery has no stray pixels
- Confirm Text is readable on the phone back
- Confirm Eye no longer feels like a short repeated loop
- Confirm Level ball movement is intuitive and center lock is believable
- Confirm Compass moves smoothly without thick flicker
- Confirm Timer remains readable and does not flash on completion
- Confirm widget left opens the inline Toy settings panel and right opens the full app settings screen
- Confirm quick settings do not expose misleading brightness controls after the widget/settings cleanup is complete
- Confirm AOD / system Glyph behavior does not fight the app

## This pass status

This pass produced code and documentation changes focused on render quality, transition design, text rendering, Eye behavior, Level behavior, and Dice/Coin polish. A fresh physical phone validation for those specific changes was not completed in this turn, so they still require manual visual confirmation.

## 2026-06-06 Widget/Tuner Continuation Smoke

Additional commands executed after the widget renderer and Tuner changes:

1. `./gradlew.bat assembleDebug`
2. `Get-Content ./todos.json -Raw | ConvertFrom-Json | Out-Null`
3. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
4. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity`
5. Debug activation/deactivation intents for `dice` and `tuner`
6. Temporary fake-fallback build with `app/libs/glyph-matrix-sdk-2.0.aar` renamed out of the Gradle `libs` tree, then restored

Results:

- `assembleDebug` passed with the real AAR present.
- `todos.json` still parses cleanly.
- APK reinstall returned `Success`.
- App launch succeeded.
- Runtime logcat showed real SDK mode, device target `Glyph.DEVICE_25111p` / `A069P`, detected matrix `13x13`, and `registered=true`.
- Dice activation entered transition, active Toy rendering, sensor startup, and clean deactivation.
- Tuner activation/deactivation entered transition and active Toy rendering without requesting microphone permission.
- After deactivation, default display selected `clock` when app settings had default mode set to clock.
- `assembleDebug` also passed while the Nothing SDK AAR was temporarily absent, confirming compile-time fake-fallback readiness.
- Launcher widget visual smoke passed: the widget shows one compact card, visible side/top/bottom hints, red active styling, and an LED-only preview.
- Launcher widget interaction was retested after replacing the old overlapping `FrameLayout` zone model with non-overlapping `TextView` zones. UIAutomator exposed the live 2x2 widget bounds on Nothing Launcher, and physical ADB taps against those bounds verified:
  - top zone -> `PREVIOUS_TOY`
  - bottom zone -> `NEXT_TOY`
  - left zone -> `OPEN_TOY_SETTINGS`
  - center zone -> toggle or panel close depending on the current widget state
- The right zone is now wired to open the full app settings screen through an Activity `PendingIntent`, and the broadcast fallback was also changed to launch the app settings route. One clean physical right-zone tap still needs to be repeated from the exact widget page because the previous follow-up tap landed after the launcher page changed.

## 2026-06-06 Real Glyph Matrix and Circular Visual Smoke

Additional commands executed after the circular widget/Toy visual pass:

1. `./gradlew.bat assembleDebug`
2. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
3. Debug activation/deactivation intents for `dice`, `battery`, `compass`, and `clock`
4. A temporary fake-fallback build with `app/libs/glyph-matrix-sdk-2.0.aar` moved out of `app/libs`, then restored

Results:

- `assembleDebug` passed with the real Nothing SDK AAR present.
- Runtime logcat showed real SDK mode, target `Glyph.DEVICE_25111p`, Nothing model `A069P`, detected matrix `13x13`, and service registration `registered=true`.
- Dice, Battery, Compass, and Clock activation entered the service transition phase, then active Toy rendering, then clean deactivation.
- Activation/deactivation animation work occurred in `GlyphHubToyService` and `GlyphTransitionEngine`; the widget only changed visual state and did not perform Matrix animation itself.
- After deactivation, the configured default display returned to `clock`.
- `assembleDebug` passed while the Nothing SDK AAR was temporarily absent, confirming that the fake/stub controller path still compiles.
- The SDK AAR was restored to `app/libs/glyph-matrix-sdk-2.0.aar`.
- A final real-AAR `assembleDebug` was run after the fake-fallback test, and that APK was installed back onto the phone.
- The app settings route used by the widget right-zone Activity `PendingIntent` was verified with `am start -n com.pelikan.glyphhub/.MainActivity --es route app_settings` and UIAutomator. The visible screen contained `App Settings`, `DEFAULT DISPLAY`, `AOD`, `Sensors`, and `Matrix intensity`.

## Current conclusion

The current build is installable and launchable on the authorized Nothing `A069P` target, compiles with and without the real Nothing SDK AAR, and exercises the real Glyph Matrix service path without an immediate crash in filtered logcat. Remaining work is physical visual judgement on the phone back, AOD/system-settings validation, and one clean right-zone widget tap validation.

## 2026-06-07 UI, Preview, Widget, and All-Toy Smoke

Additional work and tests after the UI/preview pass:

1. `./gradlew.bat assembleDebug`
2. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
3. Sequential debug activation/deactivation smoke for every registry Toy except `idle_default`: `dice`, `coin`, `rps`, `timer`, `school_class_timer`, `level`, `lux_meter`, `compass`, `tuner`, `clock`, `battery`, `eye`, `maze`, `weather`, `pixel_art`, and `text_scroll`
4. UIAutomator launcher dump from the page containing the GlyphHub widget
5. Physical ADB taps against live widget bounds:
   - top -> `PREVIOUS_TOY`
   - bottom -> `NEXT_TOY`
   - left -> `OPEN_TOY_SETTINGS`
   - center -> `TOGGLE_SELECTED_TOY`
   - right -> full app `App Settings`
6. Temporary fake-fallback build with `app/libs/glyph-matrix-sdk-2.0.aar` moved out of `app/libs`, then restored
7. Final real-AAR `assembleDebug` and reinstall

Results:

- Build passed with the real Nothing SDK AAR.
- Build passed without the Nothing SDK AAR, confirming the fake fallback still compiles.
- Final APK with the real AAR was installed back onto the phone.
- All tested Toy IDs activated through the real service path without `FATAL`, `tick failed`, or `render rejected` in the filtered log window.
- Widget right-zone behavior is now physically validated from the launcher page: it opens the full app settings route.
- The widget was returned to the launcher page and the active Toy was deactivated after testing.
- Current app settings on the test phone had `aod=false` and default display `off`, so deactivation correctly logged `default display cleared mode=off aod=false`.
- UI previews now use stable per-Toy preview frames instead of calling each Toy runtime `onTick(0L)` from widget/cards/settings.

Still not fully provable through ADB:

- Final perceived smoothness and flicker quality on the physical back Matrix.
- Nothing OS AOD selection flow in system settings.
- Long-run soak while switching Toys manually over several minutes.

## 2026-06-07 App and Widget Preview Polish Retest

Additional commands after the preview-size and icon-pattern fix:

1. `./gradlew.bat assembleDebug`
2. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
3. `adb -s <serial> shell am force-stop com.pelikan.glyphhub`
4. `adb -s <serial> shell monkey -p com.pelikan.glyphhub 1`
5. Launcher and app screenshots captured with `adb exec-out screencap -p`
6. UIAutomator dump from the launcher page containing the 2x2 GlyphHub widget
7. Physical ADB tap on the widget bottom zone, followed by a top-zone tap to return the carousel to the previous Toy

Results:

- Final `assembleDebug` passed.
- APK reinstall returned `Success`.
- The Home selected Toy preview now renders a fixed-size LED-only Dice icon instead of an oversized blank block.
- Toy grid cards now render visible per-Toy icons for Dice, Coin, Timer, and Level in the tested viewport.
- The selected-state label no longer wraps from `SELECTED` into a cut two-line state; the grid now uses compact `SEL`.
- The launcher widget preview is centered, smaller, LED-only, and unclipped. Verified live widget bounds:
  - `widget_preview`: `[263,331][425,493]`
  - `widget_toy_name`: `[242,493][447,565]`
  - `widget_center_zone`: `[242,304][447,593]`
- Widget carousel bottom tap moved Dice -> Coin and rendered the Coin-specific preview; the top tap returned the carousel to the previous Toy.
- The phone was returned to the launcher widget page after testing.

## 2026-06-07 Full Toy List and Flicker Cleanup Retest

Additional commands after the requested full-list audit and object-specific preview pass:

1. `Get-Content ./todos.json -Raw | ConvertFrom-Json | Out-Null`
2. `./gradlew.bat assembleDebug`
3. Registry-to-preview coverage check for every registered `ToyRegistry` module id
4. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
5. `adb -s <serial> shell monkey -p com.pelikan.glyphhub 1`
6. App and launcher screenshots captured with `adb exec-out screencap -p`

Results:

- `todos.json` parsed successfully.
- `assembleDebug` passed.
- The preview branch coverage check passed for all 17 registered modules: `dice`, `coin`, `rps`, `timer`, `school_class_timer`, `level`, `lux_meter`, `compass`, `tuner`, `clock`, `battery`, `eye`, `maze`, `weather`, `pixel_art`, `text_scroll`, and `idle_default`.
- APK reinstall returned `Success`.
- Home and widget screenshots confirmed that Dice preview is now a square dice symbol rather than a circular placeholder.
- Home and widget screenshots confirmed that the app/widget preview area is not black for the selected Dice Toy.
- Hard global brightness flicker/pulse was removed from normal render loops for Dice, Coin, Battery, Timer completion, RPS reveal, Maze completion, School Timer completion, Weather variants, and PixelArt default display.
- `docs/TOY_COMPLETENESS_AUDIT.md` now records which requested Toys are live, partial, or missing.
- The phone was left on the launcher page containing the GlyphHub widget.

## 2026-06-07 Animation, Sensor, PixelArt, Default Display Retest

Additional commands after the animation-radius and PixelArt icon/editor pass:

1. `.\gradlew.bat assembleDebug`
2. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk`
3. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity`
4. UIAutomator dump and ADB taps to enable `AOD` and `Sensors` from the Home screen.
5. Debug activation intents for `compass`, `level`, `lux_meter`, `dice`, `coin`, and hidden `maze`.
6. SharedPreferences test setup for `defaultDisplayMode=battery`, followed by debug `sync`.
7. PixelArt settings route launch with `--es route toy_settings --es toyId pixel_art`.
8. One physical editor tap, then SharedPreferences verification for `toy.pixel_art.selectedGlyphAsset` and `toy.pixel_art.customGlyphRows`.
9. Widget refresh, launcher screenshot, UIAutomator dump, and physical left/right widget-zone taps.
10. Temporary fake-fallback build with `app/libs/glyph-matrix-sdk-2.0.aar` moved out of `app/libs`, then restored, followed by a final real-AAR build/install.

Results:

- `assembleDebug` passed.
- `assembleDebug` passed both with the real Nothing SDK AAR present and with the AAR temporarily absent.
- APK reinstall returned `Success`.
- Device remained the intended target: Nothing Phone (4a) Pro (`A069P`, `FroggerPro`).
- Manifest registration still exposes one official `com.nothing.glyph.TOY` service: `com.pelikan.glyphhub/.glyph.GlyphHubToyService`.
- `sensorsEnabled=true` and `aodEnabled=true` were confirmed in app SharedPreferences before sensor/default-display tests.
- Sensor smoke results:
  - `compass`: `sensor availability toy=compass accel=true magnetic=true rotation=true light=true`; first events observed for sensor types `1`, `5`, `2`, and `11`.
  - `level`: first accelerometer, magnetic, rotation-vector, and light events observed.
  - `lux_meter`: first light-sensor event observed.
  - `dice`: sensor startup and first event flow observed.
  - `coin`: sensor startup and first event flow observed.
  - hidden `maze`: sensor startup and first event flow observed.
- Default display smoke:
  - With `aodEnabled=true` and `defaultDisplayMode=battery`, service sync logged `phase inactive -> default_display reason=default:battery` and `default display module=battery mode=battery`.
- PixelArt/editor smoke:
  - PixelArt settings showed the heart preset and the custom physical 13x13 editor.
  - After one editor tap, SharedPreferences contained `toy.pixel_art.selectedGlyphAsset=custom`.
  - `toy.pixel_art.customGlyphRows` saved 13 slash-separated rows.
  - The launcher widget rendered the PixelArt custom heart preview instead of a black/blank preview.
- Widget mapping smoke:
  - Right zone opened `com.pelikan.glyphhub/.MainActivity`.
  - Left zone opened the inline widget quick panel with `PIX / GLYPH / CUSTOM`.
  - UIAutomator confirmed non-overlapping widget bounds for top, bottom, left, right, and center zones.
- The final installed APK on the phone was rebuilt with the real AAR restored.

Still not fully provable through ADB:

- Human-visible smoothness/flicker of the phone-back LEDs during every transition.
- Nothing OS system settings selection path for Always-on Glyph Toy.
- Long-run soak with manual widget switching and physical phone movement.

## 2026-06-07 App Completion Pass Build / Install Smoke

Commands executed after the completion-pass implementation:

1. `.\gradlew.bat :app:compileDebugKotlin *> build_compile_output.txt`
2. `.\gradlew.bat assembleDebug *> build_assemble_output.txt`
3. `Get-Content .\build_compile_output.txt -Tail 160`
4. `Get-Content .\build_assemble_output.txt -Tail 160`
5. `Get-Content .\todos.json -Raw | ConvertFrom-Json | Out-Null`
6. `adb devices`
7. `adb logcat -c`
8. `.\gradlew.bat installDebug *> build_install_output.txt`
9. `adb shell monkey -p com.pelikan.glyphhub 1`
10. finite `adb logcat -d | findstr GlyphHub`
11. exact fatal scan: `adb logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- `:app:compileDebugKotlin`: passed.
- `assembleDebug`: passed.
- `todos.json` parse validation: passed.
- Authorized target (Nothing Phone (4a) Pro) remained available in `adb devices`.
- `installDebug`: passed; Gradle installed `app-debug.apk` on `A069P - 16`.
- `monkey -p com.pelikan.glyphhub 1`: passed; one launch event injected.
- Filtered GlyphHub/Nothing logcat showed service package replacement and `GlyphToyController` re-registration of `com.pelikan.glyphhub/.glyph.GlyphHubToyService`.
- Exact `FATAL EXCEPTION` phrase search returned no lines.

Not validated in this smoke:

- Tuner microphone pitch response.
- Sound meter microphone RMS/dB response.
- Metronome timing/haptic/sound behavior.
- Timer/Pomodoro end feedback.
- Weather fetch/cache/offline behavior.
- Web portal routes over LAN.
- Notification listener opt-in and app filtering.
- NFC/Wallet-active triggers.
- Charging, volume, network, beacon, and wake status events on the physical Matrix.
- Human-visible Matrix smoothness/readability for the newly added modules.

Important dependency note:

- OkHttp `5.3.2` was not used because it failed this Kotlin `2.0.21` build due Kotlin metadata `2.2`; the implemented Open-Meteo client uses OkHttp `4.12.0`.

## 2026-06-07 Battery Toy Repair Smoke

Commands executed after Battery renderer repair:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\device-check.ps1`
3. `adb devices -l`
4. `adb shell dumpsys battery`
5. `.\scripts\install-debug.ps1 -Serial <serial>`
6. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command activate --es toyId battery`
7. `adb -s <serial> logcat -d -s GlyphHub`

Results:

- Build passed.
- Install passed.
- Target remained Nothing Phone (4a) Pro (`A069P`, `FroggerPro`).
- Battery service state before activation: `level=77`, `USB powered=true`, `status=2`.
- Logcat confirmed `sdkMode=real`, `Real Glyph SDK service connected registered=true`, and `activated toy=battery`.
- Battery Toy was left active on the phone for direct visual inspection.

Expected visual after this repair:

- Vertical 13x13 battery silhouette with top terminal.
- Full-intensity outline LEDs.
- Fill rows at lower intensity, mapped to battery percentage.
- While charging and not full, the top fill row/next row pulses to show charge motion.
- At 100%, all inner fill rows render at full intensity.

Follow-up tuning after direct visual feedback:

- Fill rows are now exactly 1 LED high and 5 LEDs wide.
- The battery has eight internal row segments for finer percentage mapping.
- Outline intensity is fixed at 100 per pixel.
- Normal fill intensity is fixed at 68 per pixel to separate it visually from the outline on physical LEDs.
- Charging animation no longer toggles rows on/off; it uses a smooth sine pulse from intensity 16 to 68.
- Charging connected/disconnected status events are skipped while Battery Toy owns the Matrix, preventing a temporary status-frame glitch.

Second follow-up tuning after low-system-brightness visual feedback:

- Battery ignores old per-Toy brightness settings and always sends `frame.brightness=100`; Nothing OS system brightness remains the only global dimmer.
- Outline remains pixel intensity `100`.
- Fill rows increased to pixel intensity `82` so they stay visible at low system brightness while still below outline intensity.
- Charging pulse changed from subtle sine dimming to a visible six-step cycle: `18, 42, 70, 100, 70, 42` at 120 ms per step.

Third follow-up tuning for native charging override:

- Battery is now rendered as a service-level charging override whenever the phone reports charging and the explicit Battery Toy is not active.
- The charging override wins over normal active/default/status frames and is skipped when the Battery Toy itself is active, so the Battery Toy remains a static non-animated battery display.
- Charging override render cadence is `50 ms`; the regular toy loop stays at `120 ms`.
- Charging pulse uses a 60-step eased cycle over roughly three seconds.
- Battery outline is one pixel taller than the original repair and uses top-row optical balancing so lower outline pixels are not visually weaker than the terminal/top row.
- Normal inner fill rows are dimmer than the outline; they only reach full intensity when battery level is `95..100`.

Commands executed:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\install-debug.ps1 -Serial <serial>`
3. `adb -s <serial> shell dumpsys battery`
4. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command activate --es toyId dice`
5. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command sync`
6. `adb -s <serial> logcat -d -s GlyphHub`

Results:

- Build passed.
- Install passed.
- Battery service state during test: `level=79`, `USB powered=true`, `status=2`.
- Logcat confirmed real SDK mode and `Real Glyph SDK service connected registered=true`.
- Logcat confirmed active toy remained `dice`.
- Logcat confirmed native charging override with `charging override active over=dice level=79 reason=sync`.
- Phone was left with Dice active while charging, so the Matrix should show the charging Battery override rather than the Dice frame.

Fourth follow-up tuning for immediate charging response:

- Charging pulse now ranges from fully off (`0`) to the normal inner-fill brightness, not to full outline brightness.
- The 60-step eased pulse remains smooth, but the pulsing row no longer overpowers the static fill rows.
- Charging override now renders one frame immediately inside `ensureChargingOverrideLoop()` instead of waiting for the next toy loop tick.
- If another toy loop is already running, charging override restarts that loop into the faster `50 ms` cadence immediately.
- Added a short `WAKE_LOCK` permission and an 8 second partial wake-lock for real power-connected status events to reduce first-frame delay from Android power scheduling.

Commands executed:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\install-debug.ps1 -Serial <serial>`
3. `adb -s <serial> shell dumpsys battery`
4. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command activate --es toyId dice`
5. `adb -s <serial> shell dumpsys battery unplug`
6. `adb -s <serial> shell dumpsys battery set usb 0`
7. `adb -s <serial> shell dumpsys battery set status 3`
8. `adb -s <serial> shell dumpsys battery set usb 1`
9. `adb -s <serial> shell dumpsys battery set status 2`
10. `adb -s <serial> shell dumpsys battery reset`
11. `adb -s <serial> logcat -d -s GlyphHub`

Results:

- Build passed.
- Install passed.
- Battery state after reset returned to real USB charging: `level=80`, `USB powered=true`, `status=2`.
- Logcat confirmed active toy remained `dice`.
- Logcat confirmed charging override with `charging override active over=dice level=80 reason=sync`.
- Direct shell start of the protected Glyph service was rejected by Android with `Requires permission com.nothing.ketchum.permission.ENABLE`; this is expected for shell and not an app crash.
- Exact `FATAL EXCEPTION` scan returned no lines.

Tuner follow-up for smaller note glyph, real gradient line, and stronger lock:

- Replaced the scaled generic 5x7 note letter with dedicated 4x6 tuner letters, reducing the note height by one LED and making letters less distorted.
- Moved the string/octave digit up with the note letter and lowered its intensity.
- Reworked the tuning line gradient so intensity is mathematically strongest at center and decreases toward the edges.
- When the tuner is in lock tolerance, every pixel on the tuning line is set to max intensity.
- Lock tolerance is now capped at `3` cents for both visual full-line lock and haptic lock, following common Accu-Pitch behavior.
- Tuner lock haptic changed from the generic `18 ms` success pulse to a stronger `36 ms` pulse.
- `demo_lock` now runs the same lock haptic path as live tuning.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell am start ... --es command set_toy_setting --es toyId tuner --es key simulationMode --es value demo_lock`
5. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
6. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- `demo_lock` was left active on the phone.
- Exact `FATAL EXCEPTION` scan returned no lines.

Tuner follow-up for label spacing and full string simulation:

- Moved the string/octave digit left so the note letter and digit have a one-LED gap.
- Moved the tuning line to physical row `y=8`, where all `x=0..12` LEDs exist on the circular Matrix.
- Kept the marker on `y=7..8`, avoiding non-physical edge pixels and reducing the broken-line look.
- Fixed a marker edge case where a zero-weight neighboring marker could dim the full-lock line.
- Added `demo_all`, cycling through `E2`, `A2`, `D3`, `G3`, `B3`, and `E4`; each string sweeps into center, holds lock, then advances.
- Added tests for real gradient behavior and full-lock line brightness.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell am start ... --es command set_toy_setting --es toyId tuner --es key simulationMode --es value demo_all`
5. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
6. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- `demo_all` was left active on the phone for full string visual inspection.
- Exact `FATAL EXCEPTION` scan returned no lines.

Tuner follow-up for centered bright label, 3-LED marker, and haptic verification:

- Reworked the note label to be centered as an 8-pixel-wide group: 4-pixel note letter, 1-pixel gap, 3-pixel digit.
- Moved the label one pixel up and made both letter and digit full-intensity.
- Reduced tuner letters to 4x5 glyphs, one LED shorter than the previous 4x6 glyphs.
- Reworked the moving marker into a 3-LED vertical column on rows `7`, `8`, and `9`.
- Strengthened the non-lock line gradient: center now starts at `74`, edge falls to `20`.
- Added `pulseTunerLock()` as a stronger two-pulse waveform for lock feedback.
- Added logcat evidence when the Tuner lock haptic fires.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell am start ... --es command set_toy_setting --es toyId tuner --es key simulationMode --es value demo_all`
5. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
6. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime AudioRecord`
7. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- Log showed repeated `tuner lock haptic stableMs=... offset=...`, confirming the lock haptic branch fired.
- Exact `FATAL EXCEPTION` scan returned no lines.
- `demo_all` was left active on the phone.

Tuner follow-up for label vertical offset and stronger faster haptics:

- Moved the centered note label one LED down.
- Changed Tuner lock haptic to a stronger two-pulse waveform: `70 ms` max amplitude, `18 ms` gap, `78 ms` max amplitude.
- Reduced stable-lock delay from `260 ms` to `120 ms`.
- Reduced lock haptic cooldown from `2400 ms` to `1200 ms`.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell am start ... --es command set_toy_setting --es toyId tuner --es key simulationMode --es value demo_all`
5. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
6. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime`
7. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- Log showed `tuner lock haptic stableMs=129 offset=0.0`, confirming faster lock haptic firing.
- Exact `FATAL EXCEPTION` scan returned no lines.
- `demo_all` was left active on the phone.

Tuner production cleanup:

- Removed the debug-only `simulationMode` setting and all `demo_sweep`, `demo_all`, and `demo_lock` rendering paths.
- Removed the debug-only `set_toy_setting` command from `MainActivity`.
- Removed Tuner lock haptic log spam from production flow.
- Kept the final production Tuner visual: centered full-intensity note label, full-width physical tuning line, 3-LED vertical marker, stronger gradient, strict 3-cent lock, and lock haptic.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell pm grant com.pelikan.glyphhub android.permission.RECORD_AUDIO`
5. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
6. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime AudioRecord`
7. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- Live Tuner activated on the phone.
- Log showed `audio input initialized source=9 sampleRate=22050 buffer=3584`.
- Log showed `audio input recording started state=3`.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Phone was left with live Tuner active.

## 2026-06-08 School Class Timer Free Countdown Fix

School Class Timer follow-up for free countdown and end-of-week free state:

- Fixed the free countdown start anchor. If there was no previous lesson on the same day, countdown progress now starts from the Toy activation time instead of the last lesson from a previous day.
- This prevents the outer ring from collapsing to a single dot when enabling the Toy long before the first lesson of the day.
- If there is no future lesson left in the current ISO week, School Class Timer now renders the free state instead of a countdown to next week.
- Reworked the free state into an animated smiley: pause bars move upward and the smile arc rises from below into the final face.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell am start ... --es command activate --es toyId school_class_timer`
5. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime`
6. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- School Class Timer activated successfully on the phone.
- Log showed `activated toy=school_class_timer`.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Phone was left with School Class Timer active.

School Class Timer next-lesson reveal:

- Enabled BackTap handling for School Class Timer.
- During Break or FreeCountdown states, BackTap now reveals the next lesson label for `5 seconds`.
- The outer countdown ring remains active during the reveal.
- The inner pause icon crossfades into the next lesson label over `420 ms`.
- After the reveal timeout, the normal pause/free display returns automatically.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell am start ... --es command activate --es toyId school_class_timer`
5. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime`
6. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- School Class Timer activated successfully on the phone.
- Log showed `activated toy=school_class_timer`.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Phone was left with School Class Timer active.

Tuner live stability follow-up:

- Added a two-second hold for the last valid detected note/cents frame before falling back to the `TUN` no-signal state.
- Held signal frames are dimmed slightly so they read as stale but do not cause flicker while a string decays.
- Retuned cents smoothing: small jitter now uses stronger smoothing, medium movement is moderate, and larger movement still reacts quickly.
- Expanded the center marker to a taller lock/meter column for a more visible in-tune state.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell pm grant com.pelikan.glyphhub android.permission.RECORD_AUDIO`
5. `adb -s <serial> shell am force-stop com.pelikan.glyphhub`
6. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
7. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime AudioRecord`
8. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- Cold-start live Tuner activated on the phone.
- Log showed `audio input initialized source=9 sampleRate=22050 buffer=3584`.
- Log showed `audio input recording started state=3`.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Phone was left with live Tuner active.

Fifth follow-up tuning for power-connect ownership:

- Added a foreground-service charging monitor that checks battery state every `180 ms` while the Glyph service is alive.
- `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` now request service sync so the Glyph SDK bridge is warmed before a later charging event.
- The power receiver logs each received action for audit.
- The manifest power intent filter now has high priority.
- Charging override no longer restarts the 50 ms render loop on every repeated sync/status signal; it starts once per charging session, preventing animation phase resets and visible flicker.
- Battery fill now uses floor-based row mapping so the pulsing row is not unnecessarily stuck against the top outline at around 80%.
- `syncFromSettings()` now short-circuits directly to charging override when the phone is charging and no explicit toy is active. This removes the previous `charging_override -> default_off -> charging_override` black-frame flash.

Commands executed:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\install-debug.ps1 -Serial <serial>`
3. `adb -s <serial> logcat -d -s GlyphHub`
4. `adb -s <serial> shell dumpsys battery`
5. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Build passed.
- Install passed.
- `MY_PACKAGE_REPLACED` receiver fired after install.
- Service started and connected the real Glyph SDK bridge.
- Monitor acquired the short charging wake-lock.
- Charging override started immediately from monitor.
- Follow-up sync kept the charging override active and no longer inserted a `default_off`/black frame.
- Battery state during test: `level=80`, `USB powered=true`, `status=2`.
- Exact `FATAL EXCEPTION` scan returned no lines.

Sixth follow-up tuning for system animation bleed-through:

- Inspected `glyph-matrix-sdk-2.0.aar` with `javap`.
- Found official SDK methods `setAppMatrixFrame(...)`, `closeAppMatrix()`, and `setGlyphMatrixTimeout(boolean)`.
- Real SDK renderer now uses `setAppMatrixFrame(GlyphMatrixFrame)` instead of the lower-priority `setMatrixFrame(GlyphMatrixFrame)`.
- Renderer disables Glyph Matrix timeout after successful registration.
- Renderer closes the app-matrix layer with `closeAppMatrix()` during shutdown.
- Charging override render cadence changed from `50 ms` to `33 ms`.
- Battery pulse changed from 60 steps to 90 steps at `33 ms`, keeping roughly the same cycle duration with finer transitions.

Commands executed:

1. `jar tf app\libs\glyph-matrix-sdk-2.0.aar`
2. `javap -classpath %TEMP%\glyphsdk_inspect\classes.jar -public com.nothing.ketchum.GlyphMatrixManager`
3. `.\scripts\build-debug.ps1`
4. `.\scripts\install-debug.ps1 -Serial <serial>`
5. `adb -s <serial> logcat -d -s GlyphHub`
6. `adb -s <serial> shell dumpsys battery`
7. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Build passed.
- Install passed.
- Real SDK connected and registered.
- No Real SDK frame queued/fallback errors appeared after switching to `setAppMatrixFrame`.
- Battery state during test: `level=81`, `USB powered=true`, `status=2`.
- Exact `FATAL EXCEPTION` scan returned no lines.

## 2026-06-07 School Class Timer Repair Smoke

Implementation summary:

- Replaced the placeholder activation-relative timer with a wall-clock timetable.
- Settings now include class length, break length, day start, day end, active weekdays, subject labels, long-break placement, long-break length, and brightness.
- Active class state draws the outer Matrix ring as remaining time and renders the current subject label in the center.
- Break state draws the same outer remaining-time ring at lower intensity and renders a pause/break icon.
- Free state renders a custom free-time icon instead of the old placeholder schedule bars.
- The module is now registered as a normal Time toy instead of Experimental.

Research note:

- Checked current Glyph Matrix resources and examples. I found general Nothing Glyph Matrix tools, the official example/developer-kit repos, and generic LED matrix icon sources, but not a ready-made school timetable icon set tailored to the 13x13 Nothing Matrix. The implemented icons are custom pixel-safe Matrix drawings using the existing GlyphHub frame pipeline.

Commands executed:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\install-debug.ps1 -Serial <serial>`
3. `adb -s <serial> shell date`
4. `adb -s <serial> shell run-as com.pelikan.glyphhub ... glyphhub_settings.xml`
5. `adb -s <serial> shell dumpsys battery unplug`
6. `adb -s <serial> shell dumpsys battery set usb 0`
7. `adb -s <serial> shell dumpsys battery set status 3`
8. `adb -s <serial> shell am force-stop com.pelikan.glyphhub`
9. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command activate --es toyId school_class_timer`
10. `adb -s <serial> logcat -d -s GlyphHub`
11. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Build passed.
- Install passed.
- Test schedule was set to Sunday `23:00-23:59` so the current phone time fell inside a class period.
- School Class Timer activated successfully on the real SDK path.
- Battery state was intentionally left in simulated unplugged mode so Battery charging override does not cover the School Class Timer during visual inspection.
- Exact `FATAL EXCEPTION` scan returned no lines.

Follow-up repair for exact calendar editor and true outer ring:

- Replaced radius-based progress drawing with the actual outer physical LED ring from `GlyphMatrixLayout`.
- The ring is ordered clockwise and progress removes LEDs around the true Matrix edge, not a rectangular border or approximate circle.
- Free-time icon now uses a pause symbol inside the outer ring, matching the requested visual direction.
- Added a dedicated School Calendar editor in the School Class Timer settings screen.
- The editor provides day selection, exact start/end/subject rows per day, add/remove/reorder controls, copy-from-Monday, and clear-day actions.
- Exact schedules are stored as an auditable text format such as `MON=08:00-08:45:M,08:55-09:40:CJ;TUE=...`.
- The module computes breaks automatically as gaps between exact lessons.
- Legacy generated-day settings are hidden from normal UI and kept only as fallback if no exact schedule is stored.

Commands executed:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\install-debug.ps1 -Serial <serial>`
3. `adb -s <serial> shell run-as com.pelikan.glyphhub ... glyphhub_settings.xml`
4. `adb -s <serial> shell am force-stop com.pelikan.glyphhub`
5. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command activate --es toyId school_class_timer`
6. `adb -s <serial> logcat -d -s GlyphHub`
7. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Build passed.
- Install passed.
- Test exact schedule was set to `SUN=23:00-23:59:M`.
- School Class Timer activated successfully on the real SDK path.
- Battery state remains intentionally simulated unplugged so Battery charging override does not cover School Class Timer during inspection.
- Exact `FATAL EXCEPTION` scan returned no lines.

Eighth follow-up tuning for pre-connect system animation suppression:

- Added a black app-matrix guard while the Glyph service is alive, inactive, and not charging.
- The guard submits an all-off `setAppMatrixFrame` frame through the normal renderer and refreshes it with a `650 ms` keep-alive.
- Goal: reserve the app-matrix layer before a physical charger connection, so the system charging animation has less/no visible window before the custom Battery override starts.
- This does not disable the OS charging animation at the privileged system level; it preempts the visible Matrix layer from the app side.

Commands executed:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\install-debug.ps1 -Serial <serial>`
3. `adb -s <serial> shell dumpsys battery unplug`
4. `adb -s <serial> shell dumpsys battery set usb 0`
5. `adb -s <serial> shell dumpsys battery set status 3`
6. `adb -s <serial> shell dumpsys battery set usb 1`
7. `adb -s <serial> shell dumpsys battery set status 2`
8. `adb -s <serial> shell dumpsys battery reset`
9. `adb -s <serial> logcat -d -s GlyphHub`
10. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Build passed.
- Install passed.
- Simulated disconnect stopped charging override and rendered default-off black.
- Simulated reconnect restarted charging override from monitor.
- Battery state after reset returned to real USB charging: `level=81`, `USB powered=true`, `status=2`.
- Exact `FATAL EXCEPTION` scan returned no lines.

School Class Timer follow-up for exact grid timetable:

- Added the screenshot timetable as the default exact schedule:
  `MON=12:35-13:20:HV:ALL;TUE=11:40-12:25:IKT:EVEN,12:35-13:20:IKT:EVEN,13:30-15:05:PDT:EVEN;WED=11:40-12:25:F:ALL;THU=10:45-11:30:F:ALL,13:30-14:15:HV:ALL,14:20-15:05:HV:ALL;FRI=;SAT=;SUN=`.
- Added per-lesson week mode: `ALL`, `EVEN`, and `ODD`, using ISO week parity. Tuesday afternoon is stored as `EVEN` for the shown June 8-12, 2026 timetable week.
- Reworked School settings into a horizontal grid calendar with day rows and period columns. Day labels and cells are selectable, each day can contain multiple exact lessons, and lessons can be added, edited, removed, moved, copied from Monday, or cleared.
- The School Matrix state now uses the real physical outer LED edge as the progress ring. A segment starts with the full edge lit and LEDs disappear as the class or break elapses.
- Breaks are computed automatically from gaps between adjacent lessons.
- Free-time and widget preview now use an outer ring with a pause icon.
- Added a five-minute warning: the outer ring flashes strongly three times and calls the Android vibrator through `HapticFeedbackController`.

Commands executed:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\install-debug.ps1 -Serial <serial>`
3. `adb -s <serial> shell run-as com.pelikan.glyphhub ... glyphhub_settings.xml`
4. `adb -s <serial> shell am force-stop com.pelikan.glyphhub`
5. `adb -s <serial> shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command activate --es toyId school_class_timer`
6. `adb -s <serial> logcat -d -s GlyphHub`
7. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`
8. `adb -s <serial> shell dumpsys battery`

Results:

- Build passed.
- Install passed.
- Old saved School schedule preference was removed so the new screenshot-based default schedule loads.
- School Class Timer activated successfully on the real SDK path.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Battery state remains intentionally simulated unplugged: `USB powered=false`, `status=3`, `level=82`, so School Class Timer stays visible and is not covered by the Battery charging override.

School Class Timer follow-up for text safety, future countdown, and warning validation:

- Split the School timer into a pure timetable engine and a renderer.
- The engine now calculates classes, short breaks, and long free-time countdowns from exact wall-clock dates and times.
- Free time is no longer a static-only state. If the next lesson is hours or days away, the outer ring counts down to that next lesson using the whole interval between the previous lesson end and the next lesson start.
- The ring progress is second-based: `remainingSeconds / totalSeconds`, then mapped to the physical outer LED count.
- Added a dedicated Matrix subject label renderer:
  - Uses only safe one- or two-character display forms.
  - Uses the 3x5 font for two characters.
  - Uses the larger 5x7 glyph for one character.
  - Restricts lit label pixels to the safe inner text window and physical LEDs.
  - `IKT` renders as `I`, `PDT` renders as `P`, `HV` renders as `HV`.
- Warning behavior now depends on segment length:
  - Segments longer than five minutes warn in the last five minutes.
  - Segments five minutes or shorter warn only in the last minute.
- Added `school warning ...` log output when the warning fires, so haptic/blink triggering is auditable.
- Added debug command `set_toy_setting` for deterministic ADB tests without hand-editing shared preference XML.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell am start ... --es command set_toy_setting --es key schedule --es value 'MON=00:06-00:09:HV:ALL;...'`
5. `adb -s <serial> shell am start ... --es command activate --es toyId school_class_timer`
6. `adb -s <serial> logcat -d -s GlyphHub`
7. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`
8. `adb -s <serial> shell am start ... --es command set_toy_setting --es value '<real timetable>'`
9. `adb -s <serial> shell am start ... --es command activate --es toyId school_class_timer`

Results:

- Unit tests passed for:
  - screenshot timetable parsing
  - even/odd week filtering
  - second-accurate class progress
  - long free-time countdown to a future lesson
  - computed break gaps
  - short-segment warning threshold
  - all A-Z and 0-9 label rendering plus `HV`, `IKT`, `PDT`, `ČJ`, `CH`, and `MATH`
- Build passed.
- Install passed.
- Temporary phone schedule `MON=00:06-00:09:HV:ALL` triggered the warning on the real device.
- Log confirmed: `school warning segment=2026-06-08-00:06-00:09-HV remaining=44s threshold=60s total=180s`.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Restored the real timetable on phone:
  `MON=12:35-13:20:HV:ALL;TUE=11:40-12:25:IKT:ALL,12:35-13:20:IKT:ALL,13:30-15:05:PDT:EVEN;WED=11:40-12:25:F:ALL;THU=10:45-11:30:F:ALL,13:30-14:15:HV:ALL,14:20-15:05:HV:ALL;FRI=;SAT=;SUN=`.
- Final activation succeeded on the real SDK path.
- Battery state remains intentionally simulated unplugged: `USB powered=false`, `status=3`, `level=82`, so the School countdown remains visible.

School Class Timer visual retest and ring-mask fix:

- User observed only a single lower inner pixel, black frames, no visible ring, and no felt vibration.
- Root cause confirmed from new `school frame ... rows=...` logs: School frames were passed through `GlyphDesignSystem.clean()`, whose geometric `softCircularMask13` removes every true physical edge LED from `GlyphMatrixLayout`.
- Before fix, free countdown logged only `lit=21 bbox=4,4-8,10`, matching the pause icon plus the lower inner marker, with no edge ring.
- Replaced School renderer cleanup with `GlyphMatrixLayout.mask(frame)` so the real physical 13x13 layout is preserved.
- Removed the misleading lower inner marker from class/break/free countdown states.
- Strengthened School warning haptics from a 60 ms pulse to a three-pulse waveform: `130 ms`, `130 ms`, `190 ms`.
- Added `haptic=warning_pattern` to warning logs.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. Stable free-countdown visual retest.
5. Stable class `HV` visual retest.
6. Short warning haptic retest.
7. Restore real timetable and activate School Class Timer.

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- After fix, free countdown logged `lit=56 bbox=0,0-12,12`, with rows showing the full physical edge ring plus pause icon.
- Class `HV` logged `lit=56 bbox=0,0-12,12`, with rows showing the full physical edge ring plus centered `HV`.
- Warning log confirmed `haptic=warning_pattern`.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Restored the real timetable on phone and left School Class Timer active.

School Class Timer binary ring progress fix:

- User observed that the whole ring still looked lit because inactive edge pixels were drawn with dim background intensity.
- Removed the dim inactive ring background from `drawRemainingOuterRing`.
- Edge ring pixels are now binary for progress: active pixels use the state intensity, inactive pixels are fully off.
- Progress still maps from `remainingSeconds / totalSeconds` to `ceil(outerRing.size * progressRemaining)`.
- Added a unit test documenting the expected 36-edge-pixel mapping: full = 36, half = 18, last second = 1, finished = 0.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. Half-progress phone retest with `MON=00:14-00:34:HV:ALL`
5. Restore real timetable and activate School Class Timer.

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- Half-progress retest logged `school frame state=class:HV ... lit=38 bbox=0,0-9,12`.
- The `lit=38` count includes the centered `HV`; the edge ring is no longer fully lit/dimmed and the right-side edge pixels are off.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Restored the real timetable on phone and left School Class Timer active.

School Class Timer production cleanup and charging behavior:

- Shortened warning haptics from the long debug melody to a compact haptic pattern: quick tick, stronger tick, short thump.
- Removed debug-only `vibrate_test` command.
- Removed debug-only `set_toy_setting` command.
- Removed School frame bitmap dump logging.
- Removed the app-side charging guard black frame.
- Charging override now skips every active Toy, not only the Battery Toy.
- Charging override is allowed only when no active Toy is running, so charging animation can still appear as the default/no-toy charging state.
- Charging monitor no longer repeatedly logs skipped override while a Toy is active.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell dumpsys battery reset`
5. `adb -s <serial> shell am start ... --es command activate --es toyId school_class_timer`
6. `adb -s <serial> shell dumpsys battery`
7. `adb -s <serial> logcat -d -s GlyphHub`
8. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- Phone battery state restored to real charging: `USB powered=true`, `status=2`, `level=88`.
- School Class Timer activated successfully while the phone was charging.
- Log showed `activated toy=school_class_timer` and no `charging override` takeover after activation.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Real timetable remained stored on phone.

Seventh follow-up tuning for double flash at pulse floor:

- Charging pulse no longer emits a technical zero for the animated row.
- Pulse floor is now `3/100`, which should appear visually off but still keeps the app-matrix layer occupied.
- This avoids a likely SDK/app-matrix transparency gap where the underlying system charging row could bleed through at the exact off point.

Commands executed:

1. `.\scripts\build-debug.ps1`
2. `.\scripts\install-debug.ps1 -Serial <serial>`
3. `adb -s <serial> logcat -d -s GlyphHub`
4. `adb -s <serial> shell dumpsys battery`
5. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Build passed.
- Install passed.
- Charging override started after `MY_PACKAGE_REPLACED`.
- Battery state during test: `level=81`, `USB powered=true`, `status=2`.
- Exact `FATAL EXCEPTION` scan returned no lines.

## 2026-06-08 Tuner Toy Repair Smoke

Tuner follow-up for guitar/ukulele live tuning:

- Added `TunerToyLogic` as the testable target and display layer for Tuner.
- Kept the existing in-house YIN microphone path instead of importing a GPL tuner dependency.
- Guitar targets now support standard `E2 A2 D3 G3 B3 E4` and Drop D `D2 A2 D3 G3 B3 E4`.
- Ukulele targets now support high-G `G4 C4 E4 A4`, low-G `G3 C4 E4 A4`, and baritone `D3 G3 B3 E4`.
- The Matrix view now shows the nearest string/note label, a bottom cents marker, left/right direction hints, distance dots, and a center lock when inside tolerance.
- The preview icon now uses the same Tuner visual code path as the live toy.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell pm grant com.pelikan.glyphhub android.permission.RECORD_AUDIO`
5. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
6. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- `RECORD_AUDIO` was granted for the app.
- Tuner activated successfully on the phone and was left active for live audio testing.
- Log showed `activated toy=tuner`.
- Exact `AndroidRuntime`/`GlyphHub` filtered log returned no fatal crash lines during activation.
- Remaining manual validation: pluck real guitar/ukulele strings near the phone microphone and confirm visual direction/lock behavior on the phone back.

Tuner follow-up for `MIC` fallback and smooth live indicator:

- Added `FOREGROUND_SERVICE_MICROPHONE` and declared the Glyph service as `specialUse|microphone`.
- The service now upgrades to microphone foreground mode for Tuner and Sound Meter only, then downgrades when leaving those modules.
- Tuner and Sound Meter skip activation transitions so live audio feedback appears immediately.
- Tuner render interval is now `33 ms` instead of the default `120 ms`.
- Tuner idle/no-signal state now shows `TUN` instead of `MIC`.
- Audio input now tries `UNPROCESSED` microphone first, then falls back to `MIC`.
- Added AudioRecord start diagnostics to logcat.
- Tuner marker now uses blended intensity across neighboring LEDs for smoother sub-pixel-like movement on the physical matrix.
- Tuner lock haptic is a short one-shot pulse only after stable in-tune detection, with cooldown, so it does not continuously disturb the microphone.
- The input threshold setting now controls the YIN detector RMS gate.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell pm grant com.pelikan.glyphhub android.permission.RECORD_AUDIO`
5. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
6. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime AudioRecord`
7. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- Log showed `audio input initialized source=9 sampleRate=22050 buffer=3584`.
- Log showed `audio input recording started state=3`.
- Activation no longer goes through the transition phase for Tuner.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Tuner was left active on the phone for physical string testing.

Tuner visual follow-up for cleaner tuning line:

- Removed the side arrow glyphs and distance-dot hints from the live Tuner frame.
- The live frame now uses one bottom tuning line across the display with a dim center reference and a stronger moving marker.
- The moving marker blends intensity across neighboring LEDs, preserving smooth movement without fake off-grid pixels.
- The note label is drawn larger: letter as a shifted 5x7 glyph and string/octave digit as a compact side digit, both at lower intensity than the active marker.
- In-tune state keeps the center marker and adds a compact lock shape above the line.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
5. `adb -s <serial> logcat -d -s GlyphHub AndroidRuntime AudioRecord`
6. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- Log showed `audio input recording started state=3`.
- Exact `FATAL EXCEPTION` scan returned no lines.
- Tuner was left active on the phone with the revised visual.

Tuner follow-up for longer tuning line, simulation, and smarter auto mode:

- Extended the tuning marker range from the inner `1..11` span to the full horizontal `0..12` span.
- Moved the tuning line up from the bottom row to `y=9`, with the marker occupying `y=8..9`.
- Reduced the note label intensity so the tuning marker remains dominant.
- Added debug-only `simulationMode` values: `live`, `demo_sweep`, and `demo_lock`.
- Added a debug-only `set_toy_setting` command to switch simulation settings over ADB.
- Added target hysteresis so auto string detection does not jump targets unless the new target is materially better.
- Lowered the default input threshold from `8` to `3` so the tuner can react better when the phone is farther from the instrument.

Commands executed:

1. `.\gradlew.bat :app:testDebugUnitTest --no-configuration-cache`
2. `.\scripts\build-debug.ps1`
3. `.\scripts\install-debug.ps1 -Serial <serial>`
4. `adb -s <serial> shell pm grant com.pelikan.glyphhub android.permission.RECORD_AUDIO`
5. `adb -s <serial> shell am start ... --es command set_toy_setting --es toyId tuner --es key simulationMode --es value demo_sweep`
6. `adb -s <serial> shell am start ... --es command activate --es toyId tuner`
7. `adb -s <serial> shell am start ... --es command set_toy_setting --es toyId tuner --es key simulationMode --es value demo_lock`
8. `adb -s <serial> logcat -d | findstr /i /c:"FATAL EXCEPTION"`

Results:

- Unit tests passed.
- Build passed.
- Install passed.
- `demo_sweep` ran on the phone for visual inspection.
- `demo_lock` was enabled and left active on the phone for visual inspection.
- Exact `FATAL EXCEPTION` scan returned no lines.
