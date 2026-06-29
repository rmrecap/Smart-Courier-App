param(
    [string]$AvdName = "Pixel_6_API_34",
    [int]$Port = 5554,
    [switch]$WipeData,
    [string]$EmulatorPath = ""
)

$ErrorActionPreference = "Stop"

if (-not $EmulatorPath) {
    $androidHome = $env:ANDROID_HOME
    if (-not $androidHome) {
        $androidHome = $env:ANDROID_SDK_ROOT
    }
    if (-not $androidHome) {
        Write-Host "ANDROID_HOME not set. Checking typical paths..." -ForegroundColor Yellow
        $candidates = @(
            "$env:LOCALAPPDATA\Android\Sdk",
            "$env:USERPROFILE\Android\Sdk",
            "C:\Android\Sdk"
        )
        foreach ($c in $candidates) {
            if (Test-Path $c) {
                $androidHome = $c
                break
            }
        }
    }
    if (-not $androidHome) {
        Write-Error "Cannot find Android SDK. Set ANDROID_HOME environment variable."
        exit 1
    }
    $EmulatorPath = "$androidHome\emulator\emulator.exe"
}

if (-not (Test-Path $EmulatorPath)) {
    Write-Error "Emulator not found at $EmulatorPath"
    exit 1
}

Write-Host "=== Starting Android Emulator ===" -ForegroundColor Cyan
Write-Host "  ─ AVD: $AvdName" -ForegroundColor Gray
Write-Host "  ─ Port: $Port" -ForegroundColor Gray
Write-Host "  ─ Path: $EmulatorPath" -ForegroundColor Gray
Write-Host ""

$argsList = @(
    "-avd", $AvdName,
    "-port", $Port.ToString(),
    "-no-snapshot",
    "-netdelay", "none",
    "-netspeed", "full"
)
if ($WipeData) {
    $argsList += "-wipe-data"
}

Write-Host "Starting emulator process..." -ForegroundColor Yellow
$process = Start-Process -FilePath $EmulatorPath -ArgumentList $argsList -NoNewWindow -PassThru

Write-Host "Waiting for device to boot (this may take 2-5 minutes)..." -ForegroundColor Yellow
$bootTimeout = 300
$elapsed = 0
$booted = $false

while ($elapsed -lt $bootTimeout) {
    $result = adb devices 2>&1 | Select-String -Pattern "emulator-$Port\s+device"
    if ($result) {
        $bootResult = adb shell getprop sys.boot_completed 2>&1
        if ($bootResult -eq "1") {
            $booted = $true
            break
        }
    }
    Start-Sleep -Seconds 5
    $elapsed += 5
    if ($elapsed % 30 -eq 0) {
        Write-Host "  Still waiting... (${elapsed}s)" -ForegroundColor Gray
    }
}

if ($booted) {
    Write-Host "[OK] Android emulator booted on port $Port" -ForegroundColor Green

    Write-Host "Forwarding Firebase emulator ports..." -ForegroundColor Yellow
    adb forward tcp:8080 tcp:8080 | Out-Null
    adb forward tcp:9099 tcp:9099 | Out-Null
    adb forward tcp:9199 tcp:9199 | Out-Null
    adb forward tcp:4000 tcp:4000 | Out-Null
    Write-Host "[OK] Port forwarding configured" -ForegroundColor Green
    Write-Host ""
    Write-Host "Emulator ready for E2E tests!" -ForegroundColor Cyan
    Write-Host "Run: .\scripts\run-e2e-tests.ps1" -ForegroundColor Yellow
} else {
    Write-Host "[ERROR] Emulator failed to boot within ${bootTimeout}s" -ForegroundColor Red
    Write-Host "        Check AVD name with: emulator -list-avds" -ForegroundColor Yellow
    exit 1
}
