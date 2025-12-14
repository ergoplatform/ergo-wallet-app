# ErgoAuth Signature URL Consistency Fix

## Problem
There is an inconsistency between standard wallet and read-only wallet signatures in ErgoAuth:

- **Standard wallet**: Appends the source URL (`requestHost`) to the signed message
- **Read-only/cold wallet**: Does NOT append the URL to the signed message

This causes verification failures when dApps expect the URL to be present in the signature, with no way to detect if a read-only wallet was used.

## Root Cause
In `ErgoAuthUiLogic.kt` line 207, standard wallets sign:
```kotlin
val signedMessage = prefix + ergAuthRequest.signingMessage + ergAuthRequest.requestHost + suffix
```

However, in `ErgoAuth.kt` line 108-119, the `toColdAuthRequest()` method that serializes the request for cold/read-only wallets **does not include the `requestHost` field**:
```kotlin
fun toColdAuthRequest(): String {
    // ... 
    signingMessage?.let { root.addProperty(JSON_KEY_SIGNINGMESSAGE, signingMessage) }
    // ... other fields ...
    // ❌ requestHost is NOT included!
    return gson.toJson(root)
}
```

This means cold/read-only wallets never receive the `requestHost` to append to their signatures, creating the inconsistency.

## Solution Implemented
**Include `requestHost` in cold wallet signing requests for consistency**

### Changes Made

#### 1. Updated `ErgoAuth.kt` - `toColdAuthRequest()` method
Added `requestHost` to the serialized JSON:
```kotlin
fun toColdAuthRequest(): String {
    val gson = GsonBuilder().disableHtmlEscaping().create()
    val root = JsonObject()
    signingMessage?.let { root.addProperty(JSON_KEY_SIGNINGMESSAGE, signingMessage) }
    // ... other fields ...
    root.addProperty(JSON_KEY_REQUEST_HOST, requestHost)  // ✅ Now included!
    return gson.toJson(root)
}
```

#### 2. Added new constant
```kotlin
private const val JSON_KEY_REQUEST_HOST = "requestHost"
```

#### 3. Updated `parseErgoAuthRequestFromJson()` 
Modified to read `requestHost` from JSON for cold wallet requests:
```kotlin
fun parseErgoAuthRequestFromJson(
    jsonString: String,
    requestHost: String,
    sslValidatedBy: String?
): ErgoAuthRequest {
    val jsonObject = JsonParser().parse(jsonString).asJsonObject
    // ...
    
    // For cold wallet requests, requestHost comes from JSON
    val finalRequestHost = jsonObject.get(JSON_KEY_REQUEST_HOST)?.asString ?: requestHost
    
    return ErgoAuthRequest(
        // ...
        finalRequestHost,
        // ...
    )
}
```

## Impact
- **✅ Security**: Maintains domain verification capability for all wallet types
- **✅ Compatibility**: Both standard and cold/read-only wallets now include URL in signatures
- **✅ Consistency**: Signature verification works uniformly across all wallet types
- **✅ User Experience**: Transparent fix, no user-facing changes needed
- **✅ Backward Compatible**: Falls back to parameter `requestHost` if not in JSON

## Testing
To verify the fix:
1. Create an ErgoAuth signature request with a standard wallet
2. Create the same request with a read-only/cold wallet  
3. Both signatures should include the `requestHost` in the signed message
4. dApp verification should succeed for both wallet types
