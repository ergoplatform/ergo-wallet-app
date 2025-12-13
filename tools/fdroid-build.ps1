# F-Droid Reproducible Build Script (PowerShell)
# This ensures consistent builds for F-Droid verification

Write-Host "🔨 Starting F-Droid reproducible build..." -ForegroundColor Green

# Set reproducible timestamp from Git
$gitTimestamp = git log -1 --pretty=%ct
$env:SOURCE_DATE_EPOCH = $gitTimestamp
Write-Host "📅 Build timestamp: $gitTimestamp" -ForegroundColor Cyan

# Clean previous builds
Write-Host "🧹 Cleaning previous builds..." -ForegroundColor Yellow
.\gradlew.bat clean

# Build unsigned release APK
Write-Host "📦 Building release APK..." -ForegroundColor Yellow
.\gradlew.bat assembleErgomainnetRelease `
    "-Pandroid.injected.signing.store.file=" `
    "-Pandroid.injected.signing.store.password=" `
    "-Pandroid.injected.signing.key.alias=" `
    "-Pandroid.injected.signing.key.password="

# Calculate checksum
$apkPath = "android\build\outputs\apk\ergomainnet\release\android-ergomainnet-release-unsigned.apk"
if (Test-Path $apkPath) {
    Write-Host "✅ Build successful!" -ForegroundColor Green
    Write-Host "📍 APK location: $apkPath" -ForegroundColor Cyan
    Write-Host "🔐 SHA-256 checksum:" -ForegroundColor Cyan
    (Get-FileHash -Path $apkPath -Algorithm SHA256).Hash
} else {
    Write-Host "❌ Build failed - APK not found" -ForegroundColor Red
    exit 1
}
