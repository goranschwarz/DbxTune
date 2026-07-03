#Requires -Version 5.1
<#
.SYNOPSIS
    Build and package DbxTuneStarter for distribution.

.DESCRIPTION
    Publishes all three projects as single-file framework-dependent binaries,
    then zips them into a single downloadable package.

    Use -Clean to delete all bin\ and obj\ directories instead of building.

.PARAMETER Version
    Package version string, e.g. "1.2.0". Defaults to today's date (yyyy-MM-dd).

.PARAMETER OutDir
    Directory to write the final ZIP into. Defaults to the same directory as build.ps1.

.PARAMETER Clean
    Remove all bin\ and obj\ directories for every project and exit.
    Does not build or create a ZIP.

.EXAMPLE
    .\build.ps1
    .\build.ps1 -Version 1.2.0
    .\build.ps1 -Clean
#>
param(
    [switch] $Clean,
    [string] $Version = (Get-Date -Format "yyyy-MM-dd"),
    [string] $OutDir  = "$PSScriptRoot"
)

# ── Clean mode ────────────────────────────────────────────────────────────────
if ($Clean) {
    Write-Host ""
    Write-Host "=== DbxTuneStarter clean ===" -ForegroundColor Cyan
    Write-Host ""

    $removed = 0
    foreach ($dir in Get-ChildItem $PSScriptRoot -Recurse -Directory |
                     Where-Object { $_.Name -eq "bin" -or $_.Name -eq "obj" }) {
        # Skip directories that are inside other bin/obj (already removed by parent pass)
        if (-not (Test-Path $dir.FullName)) { continue }
        $rel = $dir.FullName.Substring($PSScriptRoot.Length + 1)
        Write-Host "  Removing $rel" -ForegroundColor DarkGray
        Remove-Item $dir.FullName -Recurse -Force
        $removed++
    }

    # Also remove the dist staging area
    $stage = Join-Path $OutDir "stage"
    if (Test-Path $stage) {
        Write-Host "  Removing stage\" -ForegroundColor DarkGray
        Remove-Item $stage -Recurse -Force
        $removed++
    }

    Write-Host ""
    Write-Host "Removed $removed director$(if ($removed -eq 1) {'y'} else {'ies'})." -ForegroundColor Green
    Write-Host ""
    return
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rid      = "win-x64"
$config   = "Release"
$zipName  = "DbxTuneStarter_$Version.zip"
$zipPath  = Join-Path $OutDir $zipName
$stage    = Join-Path $OutDir "stage"

Write-Host ""
Write-Host "=== DbxTuneStarter pack v$Version ===" -ForegroundColor Cyan
Write-Host ""

# ── Clean all bin\ and obj\ directories ──────────────────────────────────────
# A full filesystem wipe is needed because dotnet clean only removes the current
# TFM output; leftover directories from a previous TFM (e.g. net8.0-windows after
# migrating to net10.0-windows) would otherwise be picked up by the recursive
# publish-dir search below and overwrite the freshly built files.
Write-Host "Cleaning bin\ and obj\ directories..." -ForegroundColor Yellow
foreach ($dir in Get-ChildItem $PSScriptRoot -Recurse -Directory |
                 Where-Object { $_.Name -eq "bin" -or $_.Name -eq "obj" }) {
    if (-not (Test-Path $dir.FullName)) { continue }
    Remove-Item $dir.FullName -Recurse -Force
}

# ── Clean staging area ────────────────────────────────────────────────────────
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item $stage -ItemType Directory | Out-Null

# ── Publish each project ──────────────────────────────────────────────────────
$projects = @(
    "DbxInstaller\DbxInstaller.csproj",
    "DbxStarterService\DbxStarterService.csproj",
    "DbxStarterClient\DbxStarterClient.csproj"
)

foreach ($proj in $projects) {
    $name = [System.IO.Path]::GetFileNameWithoutExtension($proj)
    Write-Host "Publishing $name..." -ForegroundColor Yellow
    dotnet publish "$PSScriptRoot\$proj" -c $config -r $rid --nologo -v q
    if ($LASTEXITCODE -ne 0) { throw "publish failed for $proj" }

    # Grab every file from publish\ except .pdb.
    # With -r win-x64 the layout is bin\<config>\<tfm>\<rid>\publish\, so we
    # search recursively rather than assuming a fixed depth.
    $projDir = [System.IO.Path]::GetDirectoryName($proj)
    $binDir  = "$PSScriptRoot\$projDir\bin\$config"
    $pubDirs = Get-ChildItem $binDir -Recurse -Directory -Filter "publish" -ErrorAction SilentlyContinue
    $copied  = 0
    foreach ($pub in $pubDirs) {
        # Flat files from publish\ (skip .pdb)
        $files = @(Get-ChildItem $pub.FullName -File | Where-Object { $_.Extension -ne ".pdb" })
        $files | Copy-Item -Destination $stage
        $copied += $files.Count

        # Native support DLLs from the intermediate RID output directory (parent of publish\).
        # Packages like Scintilla5.NET set CopyToOutputDirectory but NOT CopyToPublishDirectory,
        # so their runtimes\ tree lands in the intermediate dir (<tfm>\<rid>\) but not in
        # publish\. We copy it from there so the ZIP includes the DLLs alongside the exe.
        $ridDir = Split-Path $pub.FullName -Parent
        $rtSrc  = Join-Path $ridDir "runtimes"
        if (Test-Path $rtSrc) {
            Copy-Item $rtSrc -Destination $stage -Recurse -Force
        }
    }
    if ($copied -eq 0) { throw "No files staged for $name - publish output not found under $binDir" }
}

# ── Prerequisite-check companion script ───────────────────────────────────────
# Ships next to DbxInstaller.exe as a diagnostic complement — if double-clicking the
# exe seems to do nothing (its own missing-.NET-Desktop-Runtime dialog can render
# off-screen on some machines), running this instead reports what's actually missing.
Copy-Item "$PSScriptRoot\DbxInstaller\DbxInstallerCheckReq.ps1" -Destination $stage

# ── Cleanup staging (remove files not needed on Windows) ─────────────────────
Write-Host "Cleaning up..." -ForegroundColor Yellow

$cleanupRules = @(
    @{ Pattern = "*.sh";  Desc = "Linux shell scripts" }
    # Add more rules here as needed, e.g.:
    # @{ Pattern = "*.dylib"; Desc = "macOS libraries" }
    # @{ Pattern = "*.so";    Desc = "Linux shared libraries" }
)

foreach ($rule in $cleanupRules) {
    $files = @(Get-ChildItem $stage -Filter $rule.Pattern -Recurse)
    if ($files.Count -gt 0) {
        $files | Remove-Item -Force
        Write-Host ("  Removed {0} {1}: {2}" -f $files.Count, $rule.Desc,
            ($files.Name -join ", "))
    }
}

# dotnet publish -r win-x64 "unlayers" native DLLs from runtimes/win-x64/native/ into the
# publish root as flat files.  Scintilla5.NET does NOT look there — it searches
# specifically in runtimes\<rid>\native\.  Remove the flat duplicates so they
# don't confuse users (and save ~2 MB in the ZIP).
foreach ($nativeDll in @("Scintilla.dll", "Lexilla.dll")) {
    $f = Join-Path $stage $nativeDll
    if (Test-Path $f) {
        Remove-Item $f -Force
        Write-Host "  Removed flat duplicate: $nativeDll" -ForegroundColor DarkGray
    }
}

# We publish for win-x64 only.  The Scintilla5.NET NuGet ships all three Windows
# RIDs (win-x64, win-x86, win-arm64); keep only the one we need.
$rtDir = Join-Path $stage "runtimes"
if (Test-Path $rtDir) {
    Get-ChildItem $rtDir -Directory | Where-Object { $_.Name -ne $rid } | ForEach-Object {
        Remove-Item $_.FullName -Recurse -Force
        Write-Host "  Removed unneeded runtime dir: runtimes\$($_.Name)" -ForegroundColor DarkGray
    }
}

# ── Create ZIP ────────────────────────────────────────────────────────────────
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
New-Item $OutDir -ItemType Directory -Force | Out-Null

Write-Host "Creating $zipName..." -ForegroundColor Yellow
Compress-Archive -Path "$stage\*" -DestinationPath $zipPath

# ── Summary ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "Package contents:" -ForegroundColor Cyan
Get-ChildItem $stage | ForEach-Object {
    if ($_.PSIsContainer) {
        $measure = Get-ChildItem $_.FullName -Recurse -File | Measure-Object -Property Length -Sum
        $dirKb   = if ($measure.Count -gt 0) { [math]::Round($measure.Sum / 1KB) } else { 0 }
        Write-Host ("  {0,-40} {1,8} KB  (folder, $($measure.Count) files)" -f $_.Name, $dirKb)
    } else {
        Write-Host ("  {0,-40} {1,8} KB" -f $_.Name, [math]::Round($_.Length / 1KB))
    }
}
$zipSize = [math]::Round((Get-Item $zipPath).Length / 1MB, 1)
Write-Host ""
Write-Host "Output: $zipPath  ($zipSize MB)" -ForegroundColor Green
Write-Host ""

# ── Clean up staging and old ZIPs ────────────────────────────────────────────
Remove-Item $stage -Recurse -Force

$old = @(Get-ChildItem $OutDir -Filter "DbxTuneStarter_*.zip" |
       Where-Object { $_.FullName -ne $zipPath })
if ($old.Count -gt 0) {
    $old | Remove-Item -Force
    Write-Host "Removed $($old.Count) old ZIP$(if ($old.Count -ne 1) {'s'}): $($old.Name -join ', ')" -ForegroundColor DarkGray
}

# ── Post-build clean ──────────────────────────────────────────────────────────
# ZIP is done — wipe bin\ and obj\ so the repo stays tidy.
Write-Host "Cleaning bin\ and obj\ ..." -ForegroundColor DarkGray
foreach ($dir in Get-ChildItem $PSScriptRoot -Recurse -Directory |
                 Where-Object { $_.Name -eq "bin" -or $_.Name -eq "obj" }) {
    if (-not (Test-Path $dir.FullName)) { continue }
    Remove-Item $dir.FullName -Recurse -Force
}
