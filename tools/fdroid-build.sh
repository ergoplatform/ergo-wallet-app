#!/bin/bash
# F-Droid Reproducible Build Script
# This ensures consistent builds for F-Droid verification

set -e

echo "🔨 Starting F-Droid reproducible build..."

# Set reproducible timestamp from Git
export SOURCE_DATE_EPOCH=$(git log -1 --pretty=%ct)
echo "📅 Build timestamp: $SOURCE_DATE_EPOCH"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build unsigned release APK
echo "📦 Building release APK..."
./gradlew assembleErgomainnetRelease \
    -Pandroid.injected.signing.store.file= \
    -Pandroid.injected.signing.store.password= \
    -Pandroid.injected.signing.key.alias= \
    -Pandroid.injected.signing.key.password=

# Calculate checksum
APK_PATH="android/build/outputs/apk/ergomainnet/release/android-ergomainnet-release-unsigned.apk"
if [ -f "$APK_PATH" ]; then
    echo "✅ Build successful!"
    echo "📍 APK location: $APK_PATH"
    echo "🔐 SHA-256 checksum:"
    sha256sum "$APK_PATH"
else
    echo "❌ Build failed - APK not found"
    exit 1
fi
