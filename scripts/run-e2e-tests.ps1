param(
    [string]$Module = ":core:data",
    [string]$TestClass = "com.smartcourier.core.data.sync.SyncEngineE2ETest",
    [switch]$IncludeAppTests,
    [switch]$NoEmulatorCheck
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=== Smart Courier — E2E Test Runner ===" -ForegroundColor Cyan
Write-Host ""

if (-not $NoEmulatorCheck) {
    Write-Host "Checking Firebase Emulator connectivity..." -ForegroundColor Yellow
    $emulatorRunning = $false
    try {
        $response = Invoke-WebRequest -Uri "http://10.0.2.2:8080/" -TimeoutSec 3 -UseBasicParsing
        $emulatorRunning = $response.StatusCode -eq 200
    } catch {
        $emulatorRunning = $false
    }

    if (-not $emulatorRunning) {
        Write-Host ""
        Write-Host "[ERROR] Firebase Emulator is not reachable at 10.0.2.2:8080" -ForegroundColor Red
        Write-Host "        Start the emulators first:" -ForegroundColor Red
        Write-Host "        .\scripts\start-emulators.ps1" -ForegroundColor Yellow
        Write-Host ""
        $choice = Read-Host "Would you like to start the emulators now? (Y/N)"
        if ($choice -eq "Y" -or $choice -eq "y") {
            $emuJob = Start-Job -ScriptBlock {
                param($dir) 
                Set-Location $dir
                firebase emulators:start --project smart-courier-app-e8624
            } -ArgumentList $RootDir
            Write-Host "Waiting 10 seconds for emulators to initialize..." -ForegroundColor Yellow
            Start-Sleep -Seconds 10
        } else {
            exit 1
        }
    } else {
        Write-Host "[OK] Firebase Emulator reachable at 10.0.2.2:8080" -ForegroundColor Green
    }
}

Write-Host ""

Push-Location $RootDir
try {
    Write-Host "Running E2E tests..." -ForegroundColor Cyan

    $connectedDevice = adb devices | Select-String -Pattern "`tdevice$"
    if (-not $connectedDevice) {
        Write-Host "[WARN] No connected Android device/emulator found." -ForegroundColor Yellow
        Write-Host "       E2E tests require an Android device or emulator." -ForegroundColor Yellow
        Write-Host "       Start one with: .\scripts\start-android-emulator.ps1" -ForegroundColor Yellow
        Write-Host ""
        $proceed = Read-Host "Press any key to continue anyway, or Ctrl+C to abort"
    }

    if ($IncludeAppTests) {
        Write-Host "  ─ Module: $Module + :app" -ForegroundColor Gray
        & ./gradlew connectedCheck --daemon
    } else {
        Write-Host "  ─ Module: $Module" -ForegroundColor Gray
        Write-Host "  ─ Test class filter: $TestClass" -ForegroundColor Gray
        & ./gradlew "$Module:connectedAndroidTest" -Pandroid.testInstrumentationRunnerArguments.class=$TestClass --daemon
    }

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[PASS] All E2E tests passed" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "[FAIL] E2E tests failed (exit code: $LASTEXITCODE)" -ForegroundColor Red
        Write-Host "       Check build/reports/ for test reports" -ForegroundColor Yellow
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
