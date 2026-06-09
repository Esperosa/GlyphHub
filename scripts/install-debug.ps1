param(
    [string] $Serial
)

. "$PSScriptRoot\android-env.ps1"
Set-Location (Resolve-Path "$PSScriptRoot\..")

if (-not $Serial) {
    adb devices -l
    throw "install-debug requires -Serial <target-adb-serial> to avoid installing on the wrong phone."
}

$Target = Resolve-GlyphHubAdbSerial -Serial $Serial
.\gradlew.bat assembleDebug
adb -s $Target install -r "app\build\outputs\apk\debug\app-debug.apk"
