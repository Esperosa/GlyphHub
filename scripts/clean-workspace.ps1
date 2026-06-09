$ErrorActionPreference = "Stop"

$Root = Resolve-Path "$PSScriptRoot\.."
Set-Location $Root

$RunArchive = Join-Path $Root "artifacts\runs\workspace-sweep-2026-06-07"
New-Item -ItemType Directory -Force -Path $RunArchive | Out-Null
$LooseArtifactArchive = Join-Path $Root "artifacts\runs\loose-artifacts"
New-Item -ItemType Directory -Force -Path $LooseArtifactArchive | Out-Null

$GeneratedDirs = @(
    ".gradle\8.11.1",
    ".gradle\buildOutputCleanup",
    ".gradle\configuration-cache",
    ".gradle\kotlin",
    ".gradle\vcs-1",
    ".kotlin",
    "build",
    "app\build"
)

foreach ($RelativePath in $GeneratedDirs) {
    $Path = Join-Path $Root $RelativePath
    if (Test-Path -LiteralPath $Path) {
        $ResolvedPath = Resolve-Path -LiteralPath $Path
        if (-not $ResolvedPath.Path.StartsWith($Root.Path, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove path outside workspace: $ResolvedPath"
        }

        Remove-Item -LiteralPath $ResolvedPath -Recurse -Force
        Write-Host "Removed generated directory: $RelativePath"
    }
}

$GeneratedFiles = @(
    ".gradle\file-system.probe"
)

foreach ($RelativePath in $GeneratedFiles) {
    $Path = Join-Path $Root $RelativePath
    if (Test-Path -LiteralPath $Path) {
        $ResolvedPath = Resolve-Path -LiteralPath $Path
        if (-not $ResolvedPath.Path.StartsWith($Root.Path, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove path outside workspace: $ResolvedPath"
        }

        Remove-Item -LiteralPath $ResolvedPath -Force
        Write-Host "Removed generated file: $RelativePath"
    }
}

$RootRunFilePatterns = @(
    "app-*.png",
    "pixel-art-*.png",
    "widget-*.png",
    "widget-*.xml",
    "build_*_output.txt",
    "todos.json"
)

foreach ($Pattern in $RootRunFilePatterns) {
    Get-ChildItem -LiteralPath $Root -File -Filter $Pattern | ForEach-Object {
        $Destination = Join-Path $RunArchive $_.Name
        Move-Item -LiteralPath $_.FullName -Destination $Destination -Force
        Write-Host "Archived run artifact: $($_.Name)"
    }
}

Get-ChildItem -LiteralPath (Join-Path $Root "artifacts") -File | ForEach-Object {
    $Destination = Join-Path $LooseArtifactArchive $_.Name
    Move-Item -LiteralPath $_.FullName -Destination $Destination -Force
    Write-Host "Archived loose artifact: $($_.Name)"
}

Write-Host "Workspace cleanup complete."
Write-Host "Archived root run files under: $RunArchive"
