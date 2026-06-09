param(
    [string] $Serial
)

. "$PSScriptRoot\android-env.ps1"
Set-Location (Resolve-Path "$PSScriptRoot\..")

if (-not $Serial) {
    adb devices -l
    throw "logcat-glyphhub requires -Serial <target-adb-serial> to avoid reading the wrong phone."
}

$Target = Resolve-GlyphHubAdbSerial -Serial $Serial
adb -s $Target logcat -c
adb -s $Target logcat | Select-String -Pattern "GlyphHub|AndroidRuntime|FATAL EXCEPTION"
