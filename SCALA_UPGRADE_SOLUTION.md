# Scala Version Upgrade Strategy

## Problem Statement

The project is currently locked to Scala 2.11 dependencies due to RoboVM 2.3.19 compatibility limitations:
- **Current:** RoboVM 2.3.19 + Scala 2.11 libraries
- **Issue:** Can't upgrade to Scala 2.12/2.13 which would simplify codebase and access newer library versions
- **Impact:** Multiple Scala 2.11 dependencies in `common-jvm/build.gradle`:
  - `ergo-appkit_2.11:5.0.0`
  - `kiama:core_2.11:2.1.1`
  - `kiama:library_2.11:2.1.1`

## Root Cause Analysis

RoboVM 2.3.19 (last official release from mobidevelop) was built with Scala 2.11 and its bytecode is not compatible with Scala 2.12/2.13. This forces the entire dependency chain to remain on Scala 2.11.

## Solution Options

### ⭐ Solution 1: Use Multi Backend Framework (Kotlin Multiplatform Mobile - KMM)

**Recommended for long-term maintenance**

Instead of RoboVM for iOS, migrate to Kotlin Multiplatform Mobile which is actively maintained and doesn't depend on Scala versions.

#### Implementation Steps:

1. **Phase 1: Create KMM Shared Module**
   ```kotlin
   // In settings.gradle
   include ':shared'
   
   // shared/build.gradle.kts
   plugins {
       kotlin("multiplatform")
       id("com.android.library")
   }
   
   kotlin {
       android()
       iosX64()
       iosArm64()
       iosSimulatorArm64()
       
       sourceSets {
           val commonMain by getting {
               dependencies {
                   // Shared Kotlin code
               }
           }
       }
   }
   ```

2. **Phase 2: Migrate Business Logic**
   - Move wallet logic from common-jvm to KMM shared module
   - Use expect/actual for platform-specific implementations
   - Keep ergo-appkit usage in JVM-specific code

3. **Phase 3: Update iOS Module**
   - Remove RoboVM dependency
   - Use KMM iOS framework
   - Rewrite iOS UI to call shared Kotlin code

**Pros:**
- ✅ No Scala version constraints
- ✅ Active maintenance (by JetBrains)
- ✅ Better iOS performance
- ✅ Modern Kotlin approach
- ✅ Can use latest Kotlin libraries

**Cons:**
- ❌ Significant refactoring required
- ❌ iOS UI rewrite needed (Swift/Kotlin Native)
- ❌ Learning curve for team
- ❌ ~2-3 months development time

---

### Solution 2: Fork and Update RoboVM to Scala 2.12/2.13

**Medium complexity, good for maintaining current architecture**

Fork RoboVM and rebuild it with newer Scala versions.

#### Implementation Steps:

1. **Fork RoboVM Repository**
   ```bash
   git clone https://github.com/MobiVM/robovm.git
   cd robovm
   ```

2. **Update Scala Version in Build Files**
   ```groovy
   // In robovm/pom.xml or build files
   ext.scala_version = "2.12.18" // or 2.13.12
   ```

3. **Rebuild and Publish to Local Maven**
   ```bash
   ./gradlew publishToMavenLocal
   # or
   mvn clean install
   ```

4. **Update Project Dependencies**
   ```gradle
   // build.gradle
   ext.robovm_version = "2.4.0-scala2.12-SNAPSHOT"
   
   // Add local maven
   repositories {
       mavenLocal()
       mavenCentral()
   }
   ```

5. **Upgrade Scala Dependencies**
   ```gradle
   // common-jvm/build.gradle
   api('org.ergoplatform:ergo-appkit_2.12:5.0.0') {
       exclude group: 'org.bouncycastle', module: 'bcprov-jdk15on'
       exclude group: 'org.bitbucket.inkytonik.kiama', module: 'kiama_2.12'
       exclude group: 'com.google.guava', module: 'guava'
   }
   api('com.github.MrStahlfelge.kiama:core_2.12:2.2.0')
   api('com.github.MrStahlfelge.kiama:library_2.12:2.2.0')
   ```

**Pros:**
- ✅ Keep existing architecture
- ✅ Can use Scala 2.12/2.13 libraries
- ✅ Minimal code changes
- ✅ ~2-3 weeks development time

**Cons:**
- ❌ Maintain custom RoboVM fork
- ❌ RoboVM is not actively maintained upstream
- ❌ Need to rebuild for each update
- ❌ Potential compatibility issues

---

### Solution 3: Use Alternative JVM-to-iOS Bridge (Intel Multi-OS Engine)

**Note:** Intel MOE is also discontinued, but was Scala-independent.

Not recommended due to lack of maintenance.

---

### Solution 4: Separate iOS Codebase with REST API Bridge

**Quick solution, higher maintenance cost**

Keep Scala 2.11 for desktop/Android, create independent iOS app that communicates via API.

#### Implementation Steps:

1. **Create REST API Layer**
   ```kotlin
   // In common-jvm, add REST endpoints
   @RestController
   class WalletAPI {
       @PostMapping("/wallet/create")
       fun createWallet(): WalletResponse { ... }
       
       @PostMapping("/transaction/send")
       fun sendTransaction(request: TxRequest): TxResponse { ... }
   }
   ```

2. **Create Native iOS App**
   - Pure Swift/SwiftUI application
   - Call REST API endpoints
   - No Scala dependencies

3. **Local Communication**
   - Use embedded HTTP server in iOS app
   - Or use iOS App Extensions for communication

**Pros:**
- ✅ No Scala constraints on iOS
- ✅ Modern Swift development
- ✅ Independent iOS improvements
- ✅ ~1 month development time

**Cons:**
- ❌ Code duplication (UI logic)
- ❌ Complex communication layer
- ❌ Higher maintenance burden
- ❌ Security concerns with local API

---

### ⚡ Solution 5: Upgrade Dependencies Incrementally (Quick Win)

**Hybrid approach: Keep RoboVM on 2.11, upgrade what you can**

#### Implementation Strategy:

1. **Identify Scala-Independent Code**
   - Move pure Kotlin code out of Scala dependency chains
   - Create separate modules that don't depend on ergo-appkit

2. **Use Scala 2.11 Only Where Necessary**
   ```gradle
   // common-jvm/build.gradle - Keep as Scala 2.11
   dependencies {
       api('org.ergoplatform:ergo-appkit_2.11:5.0.0')
   }
   
   // common-kotlin/build.gradle - New pure Kotlin module
   dependencies {
       implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
       // No Scala dependencies
   }
   ```

3. **Desktop Module Can Use Newer Scala**
   ```gradle
   // desktop/build.gradle.kts
   dependencies {
       // Desktop can use Scala 2.13
       implementation("org.ergoplatform:ergo-appkit_2.13:5.0.0")
   }
   ```

4. **Create Abstraction Layer**
   ```kotlin
   // Define interfaces in common-kotlin
   interface ErgoWalletService {
       fun createWallet(): Wallet
       fun sendTransaction(tx: Transaction): Result
   }
   
   // Implement in common-jvm (Scala 2.11)
   class ErgoWalletServiceImpl : ErgoWalletService { ... }
   
   // Implement in desktop (Scala 2.13)
   class DesktopErgoWalletServiceImpl : ErgoWalletService { ... }
   ```

**Pros:**
- ✅ Minimal disruption
- ✅ Progressive improvement
- ✅ Keep iOS working as-is
- ✅ ~2 weeks development time

**Cons:**
- ❌ iOS still locked to Scala 2.11
- ❌ Partial solution only
- ❌ More complex build configuration

---

## Recommended Implementation Plan

### Short Term (1-2 months): Solution 5
1. Restructure modules to isolate Scala dependencies
2. Create pure Kotlin modules for shared logic
3. Allow desktop to use Scala 2.13

### Medium Term (3-6 months): Solution 2 or Solution 1
**Option A: Fork RoboVM (if staying with current architecture)**
1. Fork RoboVM and upgrade to Scala 2.12
2. Publish to GitHub Packages or JitPack
3. Upgrade all Scala dependencies

**Option B: Migrate to KMM (if modernizing architecture)**
1. Create KMM shared module
2. Migrate business logic incrementally
3. Rewrite iOS app with KMM

### Decision Matrix

| Criteria | Solution 1 (KMM) | Solution 2 (Fork RoboVM) | Solution 5 (Hybrid) |
|----------|------------------|--------------------------|---------------------|
| Development Time | 3 months | 3 weeks | 2 weeks |
| Long-term Maintenance | Low | Medium | High |
| Scala Unification | Yes | Yes | Partial |
| iOS Performance | Better | Same | Same |
| Risk | Medium | Low | Low |
| Future Proofing | High | Medium | Low |

## Immediate Action Items

1. **Assess Team Capability**
   - Kotlin Multiplatform experience?
   - iOS Swift development experience?
   - Scala/Build system expertise?

2. **Evaluate Business Priorities**
   - Is iOS critical path? (Solution 1/2)
   - Can iOS be separate? (Solution 4)
   - Need quick fix? (Solution 5)

3. **Create POC**
   - Test Solution 2: Build RoboVM with Scala 2.12
   - Test Solution 5: Separate common-kotlin module

4. **Update Documentation**
   - Document current Scala 2.11 constraint
   - Plan migration timeline
   - Set up testing strategy

## Technical Resources

- **RoboVM Fork (MobiVM):** https://github.com/MobiVM/robovm
- **Kotlin Multiplatform:** https://kotlinlang.org/docs/multiplatform.html
- **Ergo AppKit Releases:** https://github.com/ergoplatform/ergo-appkit/releases
- **Scala 2.12 Migration Guide:** https://docs.scala-lang.org/overviews/core/collections-migration-213.html

## Testing Strategy

For any solution, ensure:
1. ✅ iOS build and run on physical devices
2. ✅ Desktop build on all platforms (Win/Mac/Linux)
3. ✅ Android build and APK generation
4. ✅ Wallet creation/restoration works
5. ✅ Transaction signing and broadcasting works
6. ✅ All unit tests pass

## Success Metrics

- [ ] Unified Scala version across all modules (2.12 or 2.13)
- [ ] iOS build time < 2 minutes (currently ~90 seconds)
- [ ] Access to latest ergo-appkit versions
- [ ] Reduced dependency conflicts
- [ ] Simplified build configuration
- [ ] Active upstream support (for KMM option)
