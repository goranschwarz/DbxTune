#Requires -Version 5.1
<#
.SYNOPSIS
    Checks DbxTune Setup's prerequisites — a complement to DbxInstaller.exe, not a launcher.

.DESCRIPTION
    DbxInstaller.exe requires the .NET Desktop Runtime just to start. If it's missing, the
    .NET apphost shows its own "you must install .NET Desktop Runtime" dialog — on some
    machines (multi-monitor, DPI scaling) that dialog renders off-screen or behind other
    windows and is easy to miss, with no other indication anything happened. If
    double-clicking DbxInstaller.exe seems to do nothing, run this script instead: it has
    no such dependency, so it can report clearly in the console what's actually missing.

    Also checks the ASP.NET Core Runtime and Java — both needed later, during
    installation, not to launch the wizard itself. The wizard's own Prerequisites page
    already re-checks both live with a Recheck button, so this is just an earlier,
    always-visible look at the same information.

    This script does not launch DbxInstaller.exe.

.EXAMPLE
    .\DbxInstallerCheckReq.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RequiredDotNetMajor = 10
$RequiredJavaMajor   = 17
$DotNetDownloadUrl   = "https://dotnet.microsoft.com/download/dotnet/$RequiredDotNetMajor.0"
$JavaDownloadUrl     = "https://docs.microsoft.com/java/openjdk/download"

Write-Host ""
Write-Host "=== DbxTune Setup - prerequisite check ===" -ForegroundColor Cyan
Write-Host ""

# ── Helpers ────────────────────────────────────────────────────────────────────

# Installed major versions of a shared .NET runtime (e.g. "Microsoft.WindowsDesktop.App"),
# checked directly against the dotnet install folder rather than via "dotnet --list-runtimes"
# — that requires dotnet.exe to be resolvable on PATH, which isn't guaranteed even when the
# runtime itself is installed.
function Get-DotNetRuntimeMajors([string] $RuntimeName) {
    $majors = @()
    foreach ($root in @("${env:ProgramFiles}\dotnet", "${env:ProgramFiles(x86)}\dotnet")) {
        $dir = Join-Path $root "shared\$RuntimeName"
        if (-not (Test-Path $dir)) { continue }
        Get-ChildItem $dir -Directory | ForEach-Object {
            if ($_.Name -match '^(\d+)\.') { $majors += [int]$matches[1] }
        }
    }
    return @($majors | Sort-Object -Unique -Descending)
}

function Write-Check([string] $Label, [bool] $Ok, [string] $Detail, [bool] $Blocking) {
    if ($Ok) {
        Write-Host ("  [OK]   {0,-28} {1}" -f $Label, $Detail) -ForegroundColor Green
    } elseif ($Blocking) {
        Write-Host ("  [FAIL] {0,-28} {1}" -f $Label, $Detail) -ForegroundColor Red
    } else {
        Write-Host ("  [WARN] {0,-28} {1}" -f $Label, $Detail) -ForegroundColor Yellow
    }
}

# ── .NET Desktop Runtime — required just to launch DbxInstaller.exe ────────────
$desktopMajors = Get-DotNetRuntimeMajors "Microsoft.WindowsDesktop.App"
$desktopOk     = $desktopMajors -contains $RequiredDotNetMajor
$desktopDetail = if ($desktopMajors.Count -gt 0) { "Found: $($desktopMajors -join ', ')" } else { "Not found" }
Write-Check ".NET $RequiredDotNetMajor Desktop Runtime" $desktopOk $desktopDetail $true

# ── ASP.NET Core Runtime — needed later, for DbxStarterService's web UI ────────
$aspMajors = Get-DotNetRuntimeMajors "Microsoft.AspNetCore.App"
$aspOk     = $aspMajors -contains $RequiredDotNetMajor
$aspDetail = if ($aspMajors.Count -gt 0) { "Found: $($aspMajors -join ', ')" } else { "Not found" }
Write-Check "ASP.NET Core $RequiredDotNetMajor Runtime" $aspOk $aspDetail $false

# ── Java — needed later, for the DBMS collectors ────────────────────────────────
$javaExe = $null
$javaCmd = Get-Command java.exe -ErrorAction SilentlyContinue
if ($javaCmd) {
    $javaExe = $javaCmd.Source
} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
}

$javaOk = $false
$javaDetail = "Not found"
if ($javaExe) {
    # "& $javaExe -version 2>&1" would wrap java's stderr output (that's where -version
    # prints to) in NativeCommandError records under $ErrorActionPreference = "Stop",
    # aborting the script even on success. Redirecting inside a cmd /c string instead
    # merges the streams in the native shell, so PowerShell only ever sees plain stdout.
    $verOutput = cmd.exe /c "`"$javaExe`" -version 2>&1"
    if (($verOutput -join "`n") -match 'version "(?:1\.)?(\d+)') {
        $javaMajor  = [int]$matches[1]
        $javaOk     = $javaMajor -ge $RequiredJavaMajor
        $javaDetail = "Java $javaMajor detected" + $(if (-not $javaOk) { " (17+ required)" } else { "" })
    } else {
        $javaDetail = "java.exe found but its version could not be determined"
    }
}
Write-Check "Java $RequiredJavaMajor+" $javaOk $javaDetail $false

Write-Host ""

# ── Report / decide ──────────────────────────────────────────────────────────
if (-not $desktopOk) {
    Write-Host "DbxInstaller.exe cannot start without the .NET $RequiredDotNetMajor Desktop Runtime." -ForegroundColor Red
    Write-Host "Download: $DotNetDownloadUrl" -ForegroundColor Red
    Write-Host ""
    exit 1
}

if (-not $aspOk -or -not $javaOk) {
    Write-Host "DbxInstaller.exe will still start and lets you re-check these on its Prerequisites" -ForegroundColor Yellow
    Write-Host "page, but installation cannot fully complete until they're resolved:" -ForegroundColor Yellow
    if (-not $aspOk)  { Write-Host "  ASP.NET Core $RequiredDotNetMajor Runtime : $DotNetDownloadUrl" -ForegroundColor Yellow }
    if (-not $javaOk) { Write-Host "  Java $RequiredJavaMajor+                  : $JavaDownloadUrl"   -ForegroundColor Yellow }
    Write-Host ""
}

if ($desktopOk -and $aspOk -and $javaOk) {
    Write-Host "All prerequisites satisfied - DbxInstaller.exe should start normally." -ForegroundColor Green
    Write-Host ""
}
