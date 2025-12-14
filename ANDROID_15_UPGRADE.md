# Android 15 (API 35) Upgrade Guide

This document describes the changes made to update the Ergo Wallet App to target Android 15 (API level 35) to comply with Google Play requirements.

## Overview

As of November 2024, Google Play requires that all new apps and app updates must target Android 15 (API level 35) or higher. This upgrade ensures compliance with these requirements.

## Changes Made

### 1. Build Configuration Updates

#### Root `build.gradle`
- **Android Gradle Plugin (AGP)**: Updated from `7.3.1` to `8.7.3`
- **Kotlin**: Updated from `1.6.10` to `1.9.25`
- **Compose**: Updated from `1.1.0` to `1.5.15`
- **Coroutines**: Updated from `1.6.0` to `1.8.1`
- **SQLDelight**: Updated from `1.5.3` to `1.5.5`
- **Navigation**: Updated from `2.4.2` to `2.8.5`

#### `android/build.gradle`
- **compileSdkVersion**: Updated from `33` to `35`
- **targetSdkVersion**: Updated from `33` to `35`
- **buildToolsVersion**: Updated from `33.0.1` to `35.0.0`
- **Java Compatibility**: Updated from Java 8 to Java 17
- **Kotlin JVM Target**: Updated from `1.8` to `17`

### 2. AndroidX Library Updates

Updated core AndroidX libraries to support Android 15:

- `androidx.core:core-ktx`: `1.9.0` → `1.15.0`
- `androidx.appcompat:appcompat`: `1.5.1` → `1.7.0`
- `androidx.recyclerview:recyclerview`: `1.2.1` → `1.3.2`
- `androidx.paging:paging-runtime`: `3.1.1` → `3.3.5`
- `com.google.android.material:material`: `1.7.0` → `1.12.0`
- `androidx.constraintlayout:constraintlayout`: `2.1.4` → `2.2.0`
- `androidx.lifecycle:*`: `2.5.1` → `2.8.7`
- `androidx.room:*`: `2.4.3` → `2.6.1`
- `androidx.work:*`: `2.7.1` → `2.10.0`
- `androidx.biometric:biometric`: `1.1.0` → `1.2.0-alpha05`
- `desugar_jdk_libs`: `1.2.0` → `2.1.3`

### 3. Edge-to-Edge UI Implementation

Android 15 enforces edge-to-edge display by default. The following changes ensure proper UI rendering:

#### `MainActivity.kt`
- Added imports for `WindowCompat` and `ViewCompat`
- Added `WindowCompat.setDecorFitsSystemWindows(window, false)` in `onCreate()`
- This allows content to draw behind system bars while preventing UI overlap

#### `res/values/themes.xml`
- Changed `statusBarColor` from `?attr/colorSurface` to `@android:color/transparent`
- Added `navigationBarColor` set to `@android:color/transparent`
- Added `windowLightNavigationBar` attribute
- Added `enforceNavigationBarContrast` and `enforceStatusBarContrast` set to `false`

These changes enable transparent system bars while maintaining proper contrast and readability.

### 4. Compose Updates

#### `common-compose/build.gradle.kts`
- Updated Compose plugin from `1.1.0` to `1.6.11`
- Updated Java compatibility from version 8 to version 17

#### `desktop/build.gradle.kts`
- Updated Compose plugin from `1.1.0` to `1.6.11`
- Updated Java compatibility from version 11 to version 17

### 5. Gradle Wrapper

The Gradle wrapper is already at version `8.11.1`, which is compatible with AGP 8.7.3.

## Android 15 Behavior Changes Addressed

### 1. Edge-to-Edge Enforcement
- ✅ Implemented transparent system bars
- ✅ Content draws behind status and navigation bars
- ✅ Proper window insets handling already exists in the codebase

### 2. Background and Foreground Services
- ✅ Reviewed `BackgroundSync.kt` - uses WorkManager properly
- ✅ No foreground services that need timeout adjustments
- ✅ No BOOT_COMPLETED receivers launching foreground services

### 3. Native Code Compatibility (16KB Page Size)
- ⚠️ The app uses native libraries through dependencies
- ℹ️ All dependencies have been updated to their latest versions
- ℹ️ Third-party library maintainers are responsible for 16KB page size compatibility

## Testing Recommendations

1. **Test on Android 15 Emulator**
   - Verify that the status bar doesn't cut off the app's header
   - Check that navigation gestures work properly
   - Ensure no clickable elements are obscured by system bars

2. **Background Sync Testing**
   - Verify that background wallet sync still functions correctly
   - Test notification delivery for balance updates
   - Verify that periodic work requests execute as expected

3. **UI/UX Testing**
   - Check all screens for proper padding and insets
   - Verify dark mode compatibility with transparent bars
   - Test on devices with different screen sizes and aspect ratios

4. **Biometric Authentication**
   - Test fingerprint/face unlock functionality
   - Verify compatibility with Android 15's updated BiometricPrompt APIs

## Build Instructions

1. Clean the project:
   ```bash
   ./gradlew clean
   ```

2. Build the project:
   ```bash
   ./gradlew assembleErgomainnetRelease
   ```

3. For testing on Android 15:
   ```bash
   ./gradlew assembleErgomainnetDebug
   ```

## Known Issues and Considerations

1. **Compose Version**: Using Compose 1.5.15 with Kotlin 1.9.25. Consider migrating to Compose Multiplatform if iOS support needs updating.

2. **Java 17 Requirement**: All modules now require Java 17. Ensure your development environment has JDK 17 or higher.

3. **ProGuard/R8**: The release build uses ProGuard. Ensure all new libraries work correctly with code obfuscation.

4. **Native Libraries**: If you encounter issues with native code on devices with 16KB page size:
   - Check dependency updates
   - Contact library maintainers
   - Consider alternative libraries if needed

## Migration Checklist

- [x] Update AGP to 8.7.3
- [x] Update Kotlin to 1.9.25
- [x] Update targetSdkVersion to 35
- [x] Update compileSdkVersion to 35
- [x] Update build tools to 35.0.0
- [x] Update all AndroidX libraries
- [x] Implement edge-to-edge UI
- [x] Update Java compatibility to version 17
- [x] Review WorkManager usage
- [x] Update Compose versions
- [ ] Test on Android 15 emulator
- [ ] Test background sync
- [ ] Test biometric authentication
- [ ] Perform full regression testing
- [ ] Submit to Google Play

## References

- [Google Play Target API Requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Android 15 Behavior Changes](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Edge-to-Edge Documentation](https://developer.android.com/develop/ui/views/layout/edge-to-edge)
- [AGP 8.7 Release Notes](https://developer.android.com/build/releases/gradle-plugin)
- [Kotlin 1.9.25 Release Notes](https://kotlinlang.org/docs/whatsnew1925.html)
