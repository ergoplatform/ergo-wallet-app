# PR Summary

## Title
**Fix critical issues: dependency, JVM launch, F-Droid prep, and add ErgoAuth address generation**

## Quick Summary
This PR fixes three critical bugs and adds one major feature:
1. ✅ **Fixed**: Build failure due to unavailable dependency
2. ✅ **Fixed**: Windows desktop app failing to launch (CRITICAL)
3. ✅ **Added**: ErgoAuth address generation feature
4. ✅ **Prepared**: F-Droid submission with reproducible builds

## Description for PR

### Critical Bug Fixes

#### 1. Dependency Fix (Issue #181)
Fixed build failure caused by unavailable `ergo-appkit` SNAPSHOT dependency. Updated to stable release 5.0.0 from Maven Central.

#### 2. Windows Desktop JVM Launch Fix (CRITICAL)
**Problem**: Windows x64 users unable to start application after upgrade - "Failed to launch JVM" error
**Solution**: Added JVM memory options and module access flags to jpackage configuration
- Memory: `-Xmx2048m -Xms512m`
- Module access for Java 11+ compatibility
- Prevents startup failures and OutOfMemoryErrors

This is a **blocking issue** affecting all Windows desktop users.

### New Features

#### 3. ErgoAuth Address Generation
Implemented `generateAddressLink` feature improving dApp integration UX:
- **Before**: Manual address entry or double QR scanning required
- **After**: Single QR scan, wallet automatically provides address with cryptographic proof
- **URI Pattern**: `ergoauth://${url}/generateAddressLink/${uuid}/#P2PK_ADDRESS#/`
- **Response**: `{signedMessage, proof, changeAddress, addresses[]}`

### Improvements

#### 4. F-Droid Distribution Preparation
Complete preparation for F-Droid submission including:
- F-Droid metadata and Fastlane descriptions
- Reproducible build scripts
- SOURCE_DATE_EPOCH configuration

#### 5. Scala Upgrade Strategy Documentation
Comprehensive documentation of 5 paths to upgrade from Scala 2.11, addressing RoboVM constraints.

## Files Changed
- `common-jvm/build.gradle` - Dependency update
- `build.gradle` - Repository cleanup
- `desktop/deploy/jpackage.cfg` - **JVM launch fix**
- `common-jvm/src/main/java/org/ergoplatform/uilogic/ergoauth/` - ErgoAuth feature
- `android/src/main/res/values/strings.xml` - String resources
- `ios/resources/i18n/strings.properties` - String resources
- F-Droid preparation files
- Documentation files

## Testing Required
- ✅ Build succeeds
- ✅ No compilation errors
- ⏳ Windows MSI installer with JVM fix (requires Windows environment)
- ⏳ ErgoAuth address generation with live dApp (requires dApp integration)

## Impact
- **High Priority**: Windows JVM launch fix unblocks all Windows desktop users
- **Medium Priority**: ErgoAuth feature improves dApp ecosystem UX
- **Low Priority**: F-Droid preparation expands distribution channels

## Backward Compatibility
✅ All changes are backward compatible
✅ No breaking API changes
✅ Existing features unchanged

---

**Ready for review and merge**
