param(
    [string]$ProjectId = "smart-courier-app-e8624",
    [switch]$ImportSeedData,
    [string]$SeedDataFile = ".\scripts\seed-data.json"
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot

Write-Host "=== Smart Courier — Firebase Emulator Suite ===" -ForegroundColor Cyan
Write-Host "Project ID: $ProjectId" -ForegroundColor Cyan
Write-Host "Root: $RootDir" -ForegroundColor Cyan
Write-Host ""

function Check-Java {
    try {
        $javaVersion = java -version 2>&1
        if ($javaVersion -match "version `"(\d+)") {
            $major = $Matches[1]
            if ($major -ge 11) {
                Write-Host "[OK] Java $major detected" -ForegroundColor Green
                return $true
            }
        }
    } catch {}
    Write-Host "[WARN] Java 11+ not found. Firebase Emulators require Java 11+." -ForegroundColor Yellow
    Write-Host "       Install from: https://adoptium.net/" -ForegroundColor Yellow
    return $false
}

function Check-Node {
    try {
        $nodeVersion = node --version
        Write-Host "[OK] Node.js $nodeVersion detected" -ForegroundColor Green
        return $true
    } catch {}
    Write-Host "[WARN] Node.js not found. Firebase CLI requires Node.js." -ForegroundColor Yellow
    Write-Host "       Install from: https://nodejs.org/" -ForegroundColor Yellow
    return $false
}

function Check-FirebaseCLI {
    try {
        $fbVersion = firebase --version
        Write-Host "[OK] Firebase CLI v$fbVersion detected" -ForegroundColor Green
        return $true
    } catch {}
    Write-Host "[WARN] Firebase CLI not found." -ForegroundColor Yellow
    Write-Host "       Install: npm install -g firebase-tools" -ForegroundColor Yellow
    return $false
}

Check-Java
Check-Node
Check-FirebaseCLI
Write-Host ""

$firebaseJson = Join-Path $RootDir "firebase.json"
if (-not (Test-Path $firebaseJson)) {
    Write-Error "firebase.json not found at $firebaseJson"
    exit 1
}

Push-Location $RootDir
try {
    Write-Host "Starting Firebase Emulators..." -ForegroundColor Cyan
    Write-Host "  ─ Auth:      http://10.0.2.2:9099" -ForegroundColor Gray
    Write-Host "  ─ Firestore: http://10.0.2.2:8080" -ForegroundColor Gray
    Write-Host "  ─ Storage:   http://10.0.2.2:9199" -ForegroundColor Gray
    Write-Host "  ─ Database:  http://10.0.2.2:9000" -ForegroundColor Gray
    Write-Host "  ─ Hosting:   http://10.0.2.2:5000" -ForegroundColor Gray
    Write-Host "  ─ Emulator UI: http://localhost:4000" -ForegroundColor Gray
    Write-Host ""

    $cmd = "firebase", "emulators:start", "--project", $ProjectId
    if ($ImportSeedData -and (Test-Path $SeedDataFile)) {
        $cmd += "--import=$SeedDataFile"
    }

    & $cmd
}
finally {
    Pop-Location
}
