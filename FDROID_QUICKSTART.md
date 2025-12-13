# F-Droid Setup - Quick Start

## What We've Done ✅

1. **Created F-Droid metadata** (`metadata/org.ergoplatform.android.yml`)
2. **Added fastlane metadata** for app store listings
3. **Enabled reproducible builds** in android/build.gradle
4. **Created build scripts** for testing (tools/fdroid-build.sh & .ps1)
5. **Updated README** with F-Droid badge

## Quick Test (Windows)

```powershell
# Test reproducible build
.\tools\fdroid-build.ps1

# Build twice and verify checksums match
.\gradlew.bat clean assembleErgomainnetRelease
Get-FileHash android\build\outputs\apk\ergomainnet\release\android-ergomainnet-release-unsigned.apk

.\gradlew.bat clean assembleErgomainnetRelease
Get-FileHash android\build\outputs\apk\ergomainnet\release\android-ergomainnet-release-unsigned.apk
# Checksums should be identical!
```

## Next Steps

1. **Review files created:**
   - `metadata/org.ergoplatform.android.yml` - F-Droid app metadata
   - `fastlane/metadata/android/en-US/` - App descriptions
   - `tools/fdroid-build.ps1` - Build script
   - `FDROID_SUBMISSION.md` - Complete guide

2. **Test locally:**
   ```powershell
   .\tools\fdroid-build.ps1
   ```

3. **Add screenshots:**
   - Place in `fastlane/metadata/android/en-US/images/phoneScreenshots/`
   - Format: 1.png, 2.png, etc. (1080x1920 recommended)

4. **Submit to F-Droid:**
   - Follow [FDROID_SUBMISSION.md](FDROID_SUBMISSION.md) guide
   - Fork https://gitlab.com/fdroid/fdroiddata
   - Create merge request with metadata

## Key Benefits

✅ **Reproducible Builds** - Anyone can verify APK matches source
✅ **Privacy-Focused** - No tracking required
✅ **FOSS Distribution** - Reach privacy-conscious users
✅ **Automatic Updates** - Once approved, updates are automatic
✅ **Independent Verification** - F-Droid independently builds and verifies

## Files Reference

```
ergo-wallet-app/
├── metadata/
│   └── org.ergoplatform.android.yml     # F-Droid metadata
├── fastlane/
│   └── metadata/android/en-US/
│       ├── title.txt                     # App name
│       ├── short_description.txt         # Brief description
│       ├── full_description.txt          # Full description
│       └── changelogs/                   # Version changelogs
├── tools/
│   ├── fdroid-build.sh                   # Linux/Mac build script
│   └── fdroid-build.ps1                  # Windows build script
├── android/
│   └── build.gradle                      # Updated with reproducible config
├── FDROID_SUBMISSION.md                  # Complete submission guide
└── README.md                             # Updated with F-Droid badge
```

## Common Commands

```powershell
# Test build
.\tools\fdroid-build.ps1

# Clean build
.\gradlew.bat clean

# Release build
.\gradlew.bat assembleErgomainnetRelease

# Check APK
Get-FileHash android\build\outputs\apk\ergomainnet\release\*.apk
```

## Need Help?

- 📖 Read: [FDROID_SUBMISSION.md](FDROID_SUBMISSION.md)
- 💬 Ask: #fdroid on Matrix or Ergo Discord
- 🐛 Report: GitHub Issues
