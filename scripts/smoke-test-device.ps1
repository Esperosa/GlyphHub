param(
    [string] $Serial
)

. "$PSScriptRoot\android-env.ps1"
Set-Location (Resolve-Path "$PSScriptRoot\..")

if (-not $Serial) {
    adb devices -l
    throw "smoke-test-device requires -Serial <target-adb-serial> to avoid testing the wrong phone."
}

$Target = Resolve-GlyphHubAdbSerial -Serial $Serial
Write-Host "Target serial: $Target"

.\gradlew.bat assembleDebug
adb -s $Target install -r "app\build\outputs\apk\debug\app-debug.apk"
adb -s $Target shell am start -n com.pelikan.glyphhub/.MainActivity

Write-Host ""
Write-Host "Installed package:"
adb -s $Target shell pm list packages com.pelikan.glyphhub

Write-Host ""
Write-Host "Device:"
adb -s $Target shell getprop ro.product.manufacturer
adb -s $Target shell getprop ro.product.model
adb -s $Target shell getprop ro.product.device
adb -s $Target shell getprop ro.build.version.release
