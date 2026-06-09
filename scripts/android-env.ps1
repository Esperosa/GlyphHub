$ErrorActionPreference = "Stop"

$Sdk = if ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

$RequiredPath = @(
    "C:\Windows\System32",
    "C:\Windows",
    "C:\Windows\System32\Wbem",
    (Join-Path $Sdk "platform-tools"),
    (Join-Path $Sdk "cmdline-tools\latest\bin")
)

$OrderedPath = @()
foreach ($Entry in $RequiredPath) {
    $OrderedPath += $Entry
}

for ($Index = $OrderedPath.Count - 1; $Index -ge 0; $Index--) {
    $Entry = $OrderedPath[$Index]
    if (-not (($env:Path -split ";") | Where-Object { $_.TrimEnd("\") -ieq $Entry.TrimEnd("\") })) {
        $env:Path = "$Entry;$env:Path"
    }
}

Write-Host "ANDROID_HOME=$env:ANDROID_HOME"

function Get-GlyphHubAuthorizedDevices {
    $Lines = adb devices -l
    $Devices = @()
    foreach ($Line in $Lines) {
        if ($Line -match "^(\S+)\s+device\b(.*)$") {
            $Devices += [PSCustomObject]@{
                Serial = $Matches[1]
                Detail = $Matches[2].Trim()
            }
        }
    }
    return $Devices
}

function Resolve-GlyphHubAdbSerial {
    param(
        [string] $Serial
    )

    $Devices = @(Get-GlyphHubAuthorizedDevices)
    if ($Serial) {
        $Match = $Devices | Where-Object { $_.Serial -eq $Serial } | Select-Object -First 1
        if (-not $Match) {
            adb devices -l
            throw "Requested device '$Serial' is not authorized or not connected."
        }
        return $Match.Serial
    }

    if ($Devices.Count -eq 1) {
        return $Devices[0].Serial
    }

    adb devices -l
    if ($Devices.Count -eq 0) {
        throw "No authorized Android device found. Confirm USB debugging RSA prompt on the target phone."
    }

    throw "Multiple authorized devices found. Re-run with -Serial <adb-serial>."
}
