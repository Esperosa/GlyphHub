# Testing

## Prerequisites

Install Android Studio or the Android command-line tools with:

- Android SDK Platform 34
- Android SDK Build Tools 34.0.0 or newer
- Platform tools for `adb`
- Android SDK Command-line Tools for `sdkmanager`
- JDK 17 or newer

The official Nothing Glyph Matrix SDK AAR currently declares `minSdkVersion 33`, so GlyphHub also uses `minSdk = 33`. This is compatible with the target Nothing Phone (4a) Pro test device.

Accept Android SDK licenses before building. On Windows, `sdkmanager.bat` is usually under `cmdline-tools/latest/bin`:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
```

If that file is missing, install Android SDK Command-line Tools from Android Studio SDK Manager, then rerun the command.

Enable USB debugging on the phone:

1. Open Android Settings.
2. Enable Developer options.
3. Enable USB debugging.
4. Connect the phone by USB.
5. Confirm the RSA fingerprint prompt on the phone.

## Windows

```powershell
adb devices
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
adb logcat | findstr GlyphHub
```

Project helper scripts are available for the same workflow:

```powershell
.\scripts\device-check.ps1
.\scripts\build-debug.ps1
```

Install, launch, logcat, uninstall, and smoke-test scripts intentionally require the exact ADB serial so they never target the wrong connected phone:

```powershell
.\scripts\device-check.ps1 -Serial YOUR_ADB_SERIAL
.\scripts\install-debug.ps1 -Serial YOUR_ADB_SERIAL
.\scripts\launch-app.ps1 -Serial YOUR_ADB_SERIAL
.\scripts\smoke-test-device.ps1 -Serial YOUR_ADB_SERIAL
.\scripts\logcat-glyphhub.ps1 -Serial YOUR_ADB_SERIAL
```

VS Code tasks are configured for build, install, device check, launch, and logcat under `.vscode/tasks.json`.

If `adb` is not in PATH on Windows, use:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

## Linux/macOS

```bash
adb devices
./gradlew assembleDebug
./gradlew installDebug
adb logcat | grep GlyphHub
```

## Widget Test

1. Install the debug APK.
2. Long press the launcher home screen.
3. Open Widgets.
4. Add the GlyphHub 2x2 widget.
5. Tap the center zone to activate/deactivate the selected Toy.
6. Tap top/bottom zones to rotate the Toy carousel.
7. Tap the left zone to open the inline Toy quick-settings panel.
8. Tap the right zone to open the full GlyphHub app settings screen.
9. In the Toy panel, tap top/bottom to change the setting, left/right to decrease/increase or cycle values, and center to accept the current value and close.

Nothing Launcher on the target device intercepted true swipe attempts for launcher navigation. Treat the tappable zones as the supported test path unless a different widget host proves reliable swipe delivery.

## Nothing Glyph Toy Test

1. Install the app.
2. Open Nothing Glyph Toys settings on the phone.
3. Select `GlyphHub`.
4. Toggle Toys from the app or widget.
5. Watch `adb logcat` for `GlyphHub` logs.

Expected real-SDK log markers on Nothing Phone (4a) Pro:

- `service created sdkMode=real`
- `Real Glyph SDK bridge initialized device=Glyph.DEVICE_25111p sdkTarget=A069P matrix=13x13`
- `Real Glyph SDK service connected registered=true`

## Debug ADB Commands

Debug APKs expose Activity intent commands so Matrix/service behavior can be tested from ADB without bypassing the service permission:

```powershell
adb shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command activate --es toyId dice
adb shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command deactivate --es toyId dice
adb shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command test_activation
adb shell am start -n com.pelikan.glyphhub/.MainActivity -a com.pelikan.glyphhub.DEBUG_COMMAND --es command refresh_widget
```

Supported `command` values are `activate`, `deactivate`, `clear`, `sync`, `test_activation`, `test_deactivation`, and `refresh_widget`. These commands are ignored in non-debuggable builds.

For physical Matrix validation, run `activate` for `dice`, `coin`, `compass`, and `clock`, then watch the phone back. ADB/logcat can prove that the SDK service is registered and frames are being sent, but final LED orientation and brightness must be confirmed visually on the hardware.

Local testing does not require Nothing Playground. Playground upload is a later manual/community publishing step after SDK integration and real-device validation.
