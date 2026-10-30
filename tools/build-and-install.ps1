param([string]$Serial)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { throw 'adb not found on PATH. Install Android platform-tools.' }
$adbArgs = @()
if ($Serial) { $adbArgs = @('-s', $Serial) }
& adb @adbArgs get-state
if ($LASTEXITCODE -ne 0) { throw 'ADB device is not available or authorized.' }
Push-Location $root
try {
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' }
    & adb @adbArgs install -r app/build/outputs/apk/debug/app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'ADB installation failed.' }
    Write-Host 'GlowSync debug APK installed. Open GlowSync on the TV to configure it.'
} finally { Pop-Location }
