# ErgoAuth Address Generation Feature

## Overview
This document describes the new ErgoAuth address generation feature that allows dApps to request wallet addresses without requiring them upfront, significantly improving user experience.

## Problem Statement
Previously, ErgoAuth required the front-end to obtain the user's address before making an authentication request. This led to poor UX where users had to:
1. Manually input their address, or
2. Scan a QR code twice (once for address, once for authentication)

## Solution
The new `generateAddressLink` feature allows dApps to request addresses directly through the ErgoAuth protocol, enabling users to:
1. Choose their preferred wallet
2. Sign a message proving ownership
3. Return their address(es) along with the proof

## Usage

### URI Pattern
```
ergoauth://${strippedUrl}/generateAddressLink/${uuid}/#P2PK_ADDRESS#/
```

### Example
```
ergoauth://example.com/generateAddressLink/abc123/#P2PK_ADDRESS#/
```

### Response Structure
When the wallet processes an address generation request, it returns a JSON object:

```json
{
  "signedMessage": "randomPrefix_ADDRESS_REQUEST_example.com_randomSuffix",
  "proof": "base64EncodedSignature",
  "changeAddress": "9fPi...primaryAddress",
  "addresses": [
    "9fPi...address1",
    "9gQj...address2",
    "9hRk...address3"
  ]
}
```

### Fields
- **signedMessage**: A unique message signed by the wallet to prove ownership
- **proof**: Base64-encoded signature proving the user owns the addresses
- **changeAddress**: The primary address (first address in the wallet)
- **addresses**: Array of all addresses from the wallet

## Implementation Details

### Detection
The wallet detects an address generation request by checking:
1. URI starts with `ergoauth://`
2. URI contains `/generateAddressLink/`
3. URI contains the `#P2PK_ADDRESS#` placeholder

### Workflow
1. **Parse Request**: Wallet detects `generateAddressLink` in the ErgoAuth URI
2. **User Confirmation**: User is prompted to select and share their address
3. **Sign Message**: Wallet signs a proof message with the primary address
4. **Build Response**: Creates response with all wallet addresses
5. **Post Response**: Sends response to dApp with address in URL (replacing `#P2PK_ADDRESS#`)

### Code Changes

#### New Constants
- `generateAddressLinkPath = "/generateAddressLink/"`
- `placeHolderP2Pk = "#P2PK_ADDRESS#"`

#### New Response Type
```kotlin
data class ErgoAuthAddressResponse(
    val signedMessage: String,
    val proof: ByteArray,
    val changeAddress: String,
    val addresses: List<String>
)
```

#### Updated Request Type
```kotlin
data class ErgoAuthRequest(
    // ... existing fields
    val isAddressRequest: Boolean = false  // NEW
)
```

## Benefits
1. **Improved UX**: No manual address entry or double QR scanning
2. **Security**: Cryptographic proof of address ownership
3. **Flexibility**: Returns all wallet addresses, not just one
4. **Consistency**: Follows similar pattern to ErgoPay's `#P2PK_ADDRESS#` placeholder

## Testing
To test the feature:

1. Create a test dApp that generates an ErgoAuth URI with `generateAddressLink`
2. Scan QR code with Ergo Wallet
3. Verify wallet shows address selection prompt
4. Confirm and verify dApp receives the response with all fields

## Compatibility
- **Backward Compatible**: Existing ErgoAuth authentication requests continue to work
- **Platform Support**: Android, iOS, and Desktop
- **Cold Wallet Support**: Works with cold signing workflow

## Security Considerations
- Wallet prompts user to confirm before sharing addresses
- Signed message includes random prefix/suffix to prevent replay attacks
- Connection security status is displayed to user
- Only responds to trusted dApps that user explicitly confirms

## Related Files
- `common-jvm/src/main/java/org/ergoplatform/uilogic/ergoauth/ErgoAuth.kt`
- `common-jvm/src/main/java/org/ergoplatform/uilogic/ergoauth/ErgoAuthUiLogic.kt`
- `common-jvm/src/main/java/org/ergoplatform/uilogic/StringResources.kt`
- `android/src/main/res/values/strings.xml`
- `ios/resources/i18n/strings.properties`
