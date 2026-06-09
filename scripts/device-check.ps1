param(
    [string] $Serial
)

. "$PSScriptRoot\android-env.ps1"
Set-Location (Resolve-Path "$PSScriptRoot\..")

adb start-server
$DeviceList = adb devices -l
$DeviceList

$Target = Resolve-GlyphHubAdbSerial -Serial $Serial

Write-Host ""
Write-Host "Target serial: $Target"
adb -s $Target shell getprop ro.product.manufacturer
adb -s $Target shell getprop ro.product.model
adb -s $Target shell getprop ro.product.device
adb -s $Target shell getprop ro.build.version.release
