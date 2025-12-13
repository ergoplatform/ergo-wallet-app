# F-Droid Submission Guide for Ergo Wallet App

## Overview

This guide will help you submit the Ergo Wallet App to F-Droid with **reproducible builds** enabled.

## Prerequisites

- [ ] GitHub repository is public
- [ ] All source code is available
- [ ] No proprietary dependencies
- [ ] Apache 2.0 License is properly declared
- [ ] Signing configuration removed from build files

## Files Created

### 1. F-Droid Metadata File
**Location:** `metadata/org.ergoplatform.android.yml`

This is the main F-Droid app description file containing:
- App metadata (name, description, license)
- Build instructions
- Update configuration

### 2. Fastlane Metadata (for app store listings)
**Location:** `fastlane/metadata/android/en-US/`
- `title.txt` - App name
- `short_description.txt` - Brief description (max 80 chars)
- `full_description.txt` - Full app description (max 4000 chars)
- `changelogs/` - Version-specific changelogs

## Step-by-Step Implementation

### Phase 1: Prepare Your Repository ✅

#### 1.1 Remove Signing Configuration

Create a non-signing build variant for F-Droid:

```gradle
// android/build.gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            // F-Droid builds unsigned APKs
            // Remove or comment out any signingConfig references
        }
    }
}
```

#### 1.2 Ensure Reproducible Dependencies

Update `build.gradle` to use specific versions (no dynamic versions like `+`):

```gradle
// ✅ Good - Specific versions
implementation 'androidx.core:core-ktx:1.9.0'

// ❌ Bad - Dynamic versions
implementation 'androidx.core:core-ktx:1.+'
```

#### 1.3 Add F-Droid Build Variant (Optional but Recommended)

```gradle
android {
    flavorDimensions "nodetype", "distribution"
    
    productFlavors {
        ergomainnet {
            dimension "nodetype"
        }
        ergotestnet {
            dimension "nodetype"
            applicationIdSuffix ".testnet"
            versionNameSuffix "-testnet"
        }
        
        // F-Droid flavor
        fdroid {
            dimension "distribution"
        }
        
        // Google Play flavor
        play {
            dimension "distribution"
        }
    }
}
```

#### 1.4 Remove Non-Free Dependencies

F-Droid doesn't allow proprietary SDKs. Check for:
- ❌ Google Play Services
- ❌ Firebase Analytics
- ❌ Crashlytics
- ❌ Proprietary ad SDKs

If you have any, create F-Droid specific versions:

```gradle
dependencies {
    // Common dependencies
    implementation 'androidx.core:core-ktx:1.9.0'
    
    // Play-specific dependencies
    playImplementation 'com.google.firebase:firebase-analytics:21.0.0'
    
    // F-Droid alternatives (FOSS)
    fdroidImplementation 'org.acra:acra-core:5.9.7'
}
```

### Phase 2: Enable Reproducible Builds 🔄

#### 2.1 Fix Build Timestamps

Add to `android/build.gradle`:

```gradle
android {
    // ... existing config
    
    buildTypes.each {
        it.buildConfigField 'long', 'TIMESTAMP', System.getenv('SOURCE_DATE_EPOCH') ?: System.currentTimeMillis() + 'L'
    }
    
    // Ensure reproducible builds
    tasks.withType(AbstractArchiveTask) {
        preserveFileTimestamps = false
        reproducibleFileOrder = true
    }
}
```

#### 2.2 Lock Gradle Wrapper Version

Ensure `gradle/wrapper/gradle-wrapper.properties` has specific version:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-7.4-bin.zip
```

#### 2.3 Enable Gradle Verification

Your project already has `gradle/verification-metadata.xml` ✅

Ensure it's complete:
```bash
./gradlew --write-verification-metadata sha256 help
```

#### 2.4 Create Reproducible Build Script

**Location:** `tools/fdroid-build.sh`

```bash
#!/bin/bash
set -e

# F-Droid reproducible build script

# Set reproducible timestamp
export SOURCE_DATE_EPOCH=$(git log -1 --pretty=%ct)

# Clean build
./gradlew clean

# Build release APK
./gradlew assembleErgomainnetRelease \
    -Pandroid.injected.signing.store.file= \
    -Pandroid.injected.signing.store.password= \
    -Pandroid.injected.signing.key.alias= \
    -Pandroid.injected.signing.key.password=

# Output location
echo "APK built at: android/build/outputs/apk/ergomainnet/release/"
```

Make it executable:
```bash
chmod +x tools/fdroid-build.sh
```

### Phase 3: Test Reproducibility Locally 🧪

#### 3.1 Install F-Droid Server Tools

```bash
# Install fdroidserver
sudo apt-get install fdroidserver

# Or using pip
pip install fdroidserver
```

#### 3.2 Test Build Locally

```bash
cd /path/to/ergo-wallet-app

# Initialize F-Droid directory structure
mkdir -p fdroid-test/repo
cd fdroid-test

# Copy metadata
cp ../metadata/org.ergoplatform.android.yml metadata/

# Initialize
fdroid init

# Run build
fdroid build org.ergoplatform.android:2305
```

#### 3.3 Verify Reproducibility

Build twice and compare checksums:

```bash
# First build
./gradlew clean assembleErgomainnetRelease
cp android/build/outputs/apk/ergomainnet/release/android-ergomainnet-release-unsigned.apk build1.apk
sha256sum build1.apk > checksums.txt

# Second build (fresh)
./gradlew clean assembleErgomainnetRelease
cp android/build/outputs/apk/ergomainnet/release/android-ergomainnet-release-unsigned.apk build2.apk
sha256sum build2.apk >> checksums.txt

# Compare
cat checksums.txt
# Both checksums should be identical!
```

### Phase 4: Submit to F-Droid 📤

#### 4.1 Fork F-Droid Data Repository

```bash
git clone https://gitlab.com/fdroid/fdroiddata.git
cd fdroiddata
```

#### 4.2 Add Your App Metadata

```bash
# Copy your metadata file
cp /path/to/ergo-wallet-app/metadata/org.ergoplatform.android.yml \
   metadata/org.ergoplatform.android.yml

# Add fastlane metadata (optional but recommended)
mkdir -p metadata/org.ergoplatform.android/en-US
cp /path/to/ergo-wallet-app/fastlane/metadata/android/en-US/* \
   metadata/org.ergoplatform.android/en-US/
```

#### 4.3 Add Graphics Assets

F-Droid requires specific graphics:

```
metadata/org.ergoplatform.android/en-US/
├── images/
│   ├── icon.png (512x512 PNG)
│   ├── featureGraphic.png (1024x500 PNG)
│   ├── phoneScreenshots/
│   │   ├── 1.png (recommended: 1080x1920)
│   │   ├── 2.png
│   │   ├── 3.png
│   │   └── ... (up to 8 screenshots)
│   └── sevenInchScreenshots/ (optional)
```

Extract from your app or create:
```bash
# Extract app icon
mkdir -p metadata/org.ergoplatform.android/en-US/images
cp /path/to/ergo-wallet-app/android/src/main/res/mipmap-xxxhdpi/ic_launcher.png \
   metadata/org.ergoplatform.android/en-US/images/icon.png
```

#### 4.4 Test Metadata Validity

```bash
cd fdroiddata

# Lint check
fdroid lint org.ergoplatform.android

# Rewrite metadata (format check)
fdroid rewritemeta org.ergoplatform.android

# Build check
fdroid build -v -l org.ergoplatform.android
```

#### 4.5 Create Merge Request

```bash
# Create branch
git checkout -b add-ergo-wallet-app

# Add files
git add metadata/org.ergoplatform.android.yml
git add metadata/org.ergoplatform.android/

# Commit
git commit -m "New app: Ergo Wallet

Official Ergo blockchain wallet with reproducible builds.

License: Apache-2.0
Website: https://ergoplatform.org
Source: https://github.com/ergoplatform/ergo-wallet-app"

# Push
git push origin add-ergo-wallet-app
```

Then create a Merge Request on GitLab: https://gitlab.com/fdroid/fdroiddata/-/merge_requests/new

### Phase 5: Reproducible Build Setup 🔐

#### 5.1 Enable Reproducible Builds in Metadata

Update `metadata/org.ergoplatform.android.yml`:

```yaml
Builds:
  - versionName: 2.4.2305
    versionCode: 2305
    commit: v2.4.2305
    subdir: android
    gradle:
      - ergomainnet
    # Enable reproducible build verification
    antifeatures:
      - None  # or list any antifeatures
    build:
      - export SOURCE_DATE_EPOCH=$(git log -1 --pretty=%ct)
      - gradle assembleErgomainnetRelease

# Enable signature check
AllowedAPKSigningKeys: sha256:YOUR_SIGNING_CERT_SHA256
```

#### 5.2 Provide Official Binary for Verification

F-Droid will compare their build with your official release:

1. Upload signed APK to GitHub releases
2. F-Droid builds unsigned APK
3. F-Droid compares binaries (should match after signature strip)

#### 5.3 Add Reproducible Build Badge

Once approved, add to `README.md`:

```markdown
<a href="https://f-droid.org/packages/org.ergoplatform.android">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
         alt="Get it on F-Droid"
         height="80">
</a>

[![Reproducible Builds](https://img.shields.io/badge/Reproducible-Builds-brightgreen)](https://reproducible-builds.org/)
```

## Common Issues and Solutions

### Issue 1: Build Fails - Missing Dependencies

**Solution:** Ensure all dependencies are in `gradle/verification-metadata.xml`:
```bash
./gradlew --write-verification-metadata sha256 assembleErgomainnetRelease
```

### Issue 2: Non-Reproducible Timestamps

**Solution:** Set `SOURCE_DATE_EPOCH` in build:
```bash
export SOURCE_DATE_EPOCH=$(git log -1 --pretty=%ct)
```

### Issue 3: Build Variant Not Found

**Solution:** F-Droid uses lowercase build variant names. Update metadata:
```yaml
gradle:
  - ergomainnet  # lowercase
```

### Issue 4: Proprietary Dependencies Detected

**Solution:** Create F-Droid specific build variant without proprietary libs:
```gradle
fdroidImplementation 'alternative-foss-library'
```

### Issue 5: Submodule Issues

**Solution:** F-Droid doesn't support Git submodules well. Either:
- Remove submodules and vendor dependencies
- Or use `srclibs` in metadata

## Maintenance

### For Each New Release:

1. **Tag the release:**
   ```bash
   git tag -a v2.5.2400 -m "Release 2.5.2400"
   git push origin v2.5.2400
   ```

2. **Update metadata:**
   ```yaml
   Builds:
     - versionName: 2.5.2400
       versionCode: 2400
       commit: v2.5.2400
       # ... rest of config
   ```

3. **Add changelog:**
   ```bash
   echo "• Added new features
   • Fixed bugs
   • Performance improvements" > fastlane/metadata/android/en-US/changelogs/2400.txt
   ```

4. **Submit update:**
   - F-Droid will auto-detect new tags if `AutoUpdateMode` is set
   - Or submit manual merge request

## Benefits of F-Droid + Reproducible Builds

✅ **Trust:** Users can verify APK matches source code
✅ **Privacy:** No tracking, no analytics required
✅ **Freedom:** All dependencies must be FOSS
✅ **Discovery:** Reach privacy-conscious users
✅ **Security:** Independent security audits
✅ **Transparency:** Build process is public and verifiable

## Verification

After your app is published on F-Droid, users can verify reproducibility:

```bash
# Download F-Droid build
wget https://f-droid.org/repo/org.ergoplatform.android_2305.apk

# Download your official build
wget https://github.com/ergoplatform/ergo-wallet-app/releases/download/v2.4.2305/app-release.apk

# Strip signatures
apksigner strip-signatures fdroid-build.apk -o fdroid-unsigned.apk
apksigner strip-signatures official-build.apk -o official-unsigned.apk

# Compare
diff fdroid-unsigned.apk official-unsigned.apk
# Should be identical!
```

## Resources

- **F-Droid Documentation:** https://f-droid.org/docs/
- **Submission Guide:** https://f-droid.org/docs/Submitting_to_F-Droid/
- **Build Metadata Reference:** https://f-droid.org/docs/Build_Metadata_Reference/
- **Reproducible Builds:** https://reproducible-builds.org/
- **F-Droid Data Repo:** https://gitlab.com/fdroid/fdroiddata

## Timeline

1. **Preparation:** 1-2 weeks (fix dependencies, test builds)
2. **Submission:** 1 day (create MR)
3. **Review:** 1-4 weeks (F-Droid team review)
4. **First Build:** 1-2 days (after approval)
5. **Updates:** Automatic (if AutoUpdateMode enabled)

## Checklist

Before submitting:

- [ ] Removed all proprietary dependencies
- [ ] Build is reproducible (tested locally)
- [ ] Metadata file is valid (`fdroid lint`)
- [ ] Screenshots and graphics added
- [ ] License file exists in repository
- [ ] README has F-Droid badge ready
- [ ] Release tagged in Git
- [ ] APK tested on real device
- [ ] All linting errors fixed

## Success Criteria

✅ App appears on F-Droid: https://f-droid.org/packages/org.ergoplatform.android
✅ Reproducible builds badge shows green
✅ App updates automatically with new releases
✅ No build warnings or errors
✅ Community feedback positive

---

**Need Help?**
- F-Droid Matrix Channel: #fdroid:f-droid.org
- Ergo Discord: https://discord.gg/kj7s7nb
- GitHub Issues: https://github.com/ergoplatform/ergo-wallet-app/issues
