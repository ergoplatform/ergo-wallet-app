# Java 21+ Compatibility Fix

## Problem
When building on Windows 11 with JDK 24, the build fails with "Unsupported class file major version 68" because Gradle's Groovy script compiler doesn't fully support Java 24 yet.

**Error**: `BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_' Unsupported class file major version 68`

## Root Cause
- **Java 24 (class version 68)**: Too new for Gradle 8.11.1's Groovy compiler
- **Gradle 7.4**: Only supports Java 8-17
- **Issue**: Gradle build scripts are written in Groovy, which has delayed Java 24 support

## Solution
Use Java 17 or Java 21 to run Gradle. Upgraded Gradle to 8.11.1 for best compatibility.

### Option 1: Install Java 17 (Recommended)

1. **Download Java 17**:
   - [Microsoft Build of OpenJDK 17](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-17)
   - [Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17)

2. **Set JAVA_HOME** (temporary for build):
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.12.7-hotspot"
   $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
   ```

3. **Build**:
   ```powershell
   .\gradlew.bat clean build
   ```

### Option 2: Use Java 21

If you prefer Java 21:
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean build
```

### Option 3: Create Build Script

Create `build-with-java17.ps1`:
```powershell
# Save current Java
$oldJavaHome = $env:JAVA_HOME
$oldPath = $env:PATH

# Use Java 17 for build
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.12.7-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Build
.\gradlew.bat clean build $args

# Restore original Java
$env:JAVA_HOME = $oldJavaHome
$env:PATH = $oldPath
```

Usage:
```powershell
.\build-with-java17.ps1
```

### Changes Made

### Changes Made

**File:** `gradle/wrapper/gradle-wrapper.properties`

```diff
- distributionUrl=https\://services.gradle.org/distributions/gradle-7.4-all.zip
+ distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-all.zip
```

**File:** `build.gradle`

Added Java Toolchain configuration for consistent compilation:
```gradle
allprojects {
    // Configure Java toolchain to use Java 17 for compilation
    plugins.withId('java') {
        java {
            toolchain {
                languageVersion = JavaLanguageVersion.of(17)
            }
        }
    }
}
```

## Why This Approach?

### Gradle 8.11.1
- Latest stable Gradle with best Java support
- Better performance and build caching
- Full Java 21 support

### Java 17/21 for Building
- All project dependencies support Java 17
- Android Gradle Plugin 7.3.1 works with Java 17
- Stable and well-tested
- Recommended LTS version

### Java Toolchain
- Ensures consistent builds across environments
- Gradle can auto-download Java 17 if needed
- Compilation always uses Java 17 regardless of Gradle's Java

## Gradle Version Compatibility Matrix

| Gradle Version | Java Support (Gradle) | Java Support (Groovy Scripts) |
|----------------|-----------------------|-------------------------------|
| 7.4            | 8-17                  | 8-17                          |
| 8.5            | 8-21                  | 8-21                          |
| 8.11.1         | 8-24                  | 8-21 (Groovy limitation)      |

## Build Instructions

### ⚠️ Important: Java 24 Not Supported

If you're running Java 24, you must use Java 17 or 21 to run Gradle builds.

**Why?** Gradle 8.11.1 uses Groovy 3.0.22 which doesn't support Java 24 bytecode (class version 68). You'll see:
```
BUG! exception in phase 'semantic analysis'
Unsupported class file major version 68
```

### Option 1: Install Java 17 (Recommended)

**Download:**
- [Adoptium Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17)
- [Oracle JDK 17](https://www.oracle.com/java/technologies/downloads/#java17)

**Build:**
```bash
java -version  # Verify Java 17
.\gradlew.bat clean build
```

### Option 2: Use Java 21

**Download:**
- [Adoptium Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21)

### Option 3: Temporary JAVA_HOME (Keep Java 24 for Other Projects)

**PowerShell:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"  # Adjust path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version  # Verify Java 17
.\gradlew.bat clean build
```

**Command Prompt:**
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
java -version
gradlew.bat clean build
```

### Option 4: Build Script (Easiest)

### Clean and Build
```bash
# Clean previous builds
./gradlew clean

# Build the project
./gradlew build

# For desktop
./gradlew desktop:build

# For Android
./gradlew android:assembleDebug
```

### Verify Gradle Version
```bash
./gradlew --version
```

Expected output:
```
Gradle 8.5
Java:       21
```

## Testing

### Prerequisites
- Windows 11
- JDK 21 installed
- JAVA_HOME pointing to JDK 21

### Test Commands
```powershell
# Check Java version
java -version
11.1 on first run)
.\gradlew.bat --version

# Test build
.\gradlew.bat clean build
```

## Compatibility Notes

### Android Gradle Plugin
Gradle 8.11.1 is compatible with:
- Android Gradle Plugin 7.3.1 (current version in project) ✅
- Android Gradle Plugin 8.0+ (for future upgrades)

### Kotlin
Gradle 8.11.1 is compatible with:
- Kotlin 1.6.10 (current version) ✅
- Kotlin 1.8+ (recommended for best compatibility)
- Kotlin 2.0+ (latest features)

### Java Versions
Works with any Java version 17 or newer:
- ✅ Java 17 (used for compilation)
- ✅ Java 21 (can run Gradle)
- ✅ Java 24 (can run Gradle)

The toolchain automatically downloads Java 17 if needed.
None identified. Gradle 8.5 is stable and well-tested with Java 21.

## Alternative Solutions

If you prefer to stay on Gradle 7.4:

### Option 1: Use Java 17
```powershell
# Download and install JDK 17
# Set JAVA_HOME to JDK 17
# Then build normally
```

### Option 2: Use Gradle Toolchain (requires Gradle 8+)
This allows building with Java 17 while using JDK 21 for Gradle itself.

Add to `build.gradle`:
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```and Java 24 support
- ✅ Latest Gradle features and performance improvements
- ✅ Better dependency resolution
- ✅ Improved build cache
- ✅ Enhanced incremental compilation
- ✅ Better Kotlin DSL support
- ✅ Full Java 21 support
- ✅ Latest Gradle features and performance improvements
- ✅ Better dependency resolution
- ✅ Improved build cache
- ✅ Enhanced incremental compilation

### Migration Notes
- No code chang11.1 downloads successfully
- [ ] Project builds without errors
- [ ] Android app builds and runs
- [ ] Desktop app builds and runs
- [ ] All tests pass
- [ ] Dependencies resolve correctly

## References
- [Gradle 8.11.1 Release Notes](https://docs.gradle.org/8.11.1/release-notes.html)
- [Gradle Java Compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- [Java 21 Features](https://openjdk.org/projects/jdk/21/)
- [Java 24 Features](https://openjdk.org/projects/jdk/24/)

## Credits
Fix implemented for building Ergo Wallet on Windows 11 with JDK 21/24
- [Gradle 8.5 Release Notes](https://docs.gradle.org/8.5/release-notes.html)
- [Gradle Java Compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- [Java 21 Features](https://openjdk.org/projects/jdk/21/)

## Credits
Fix implemented for building Ergo Wallet on Windows 11 with JDK 21.
