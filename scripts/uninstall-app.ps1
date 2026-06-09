param(
    [string] $Serial
)

. "$PSScriptRoot\android-env.ps1"
Set-Location (Resolve-Path "$PSScriptRoot\..")

if (-not $Serial) {
    adb devices -l
    throw "uninstall-app requires -Serial <target-adb-serial> to avoid uninstalling from the wrong phone."
}

$Target = Resolve-GlyphHubAdbSerial -Serial $Serial
adb -s $Target uninstall com.pelikan.glyphhub
