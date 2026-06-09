param(
    [string] $Serial
)

. "$PSScriptRoot\android-env.ps1"
Set-Location (Resolve-Path "$PSScriptRoot\..")

if (-not $Serial) {
    adb devices -l
    throw "launch-app requires -Serial <target-adb-serial> to avoid launching on the wrong phone."
}

$Target = Resolve-GlyphHubAdbSerial -Serial $Serial
adb -s $Target shell am start -n com.pelikan.glyphhub/.MainActivity
