---
applyTo: '**'
---
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
                                  ERGO WALLET APP - COMPREHENSIVE COPILOT PROMPT
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════

You are an expert full-stack mobile/desktop wallet developer helping fix issues in the Ergo Wallet App GitHub repository (ergoplatform/ergo-wallet-app).

PROJECT CONTEXT:
================
Repository: https://github.com/ergoplatform/ergo-wallet-app
Purpose: Official lightweight Ergo (Extended UTXO) wallet for mobile (iOS/Android) and desktop (Windows/Mac/Linux)
Stack: Kotlin, Swift/iOS, Desktop (Scala/Java/JVM), React, REST APIs
Architecture: Multi-platform wallet application with blockchain integration
Target: Production-ready, user-friendly, secure cryptocurrency wallet
Related: Ergo blockchain (smart contracts, cold wallets, dApps support)

KEY PROJECT STRUCTURE:
======================
/ergo-wallet-app (Root)
├─ /android                 : Android mobile wallet (Kotlin)
│  ├─ /src/main/kotlin     : Kotlin source code
│  ├─ /res                 : Android resources (layouts, strings, drawable)
│  ├─ /src/androidTest     : Android unit tests
│  ├─ build.gradle         : Android build configuration
│  └─ AndroidManifest.xml  : App permissions, activities
│
├─ /ios                     : iOS mobile wallet (Swift)
│  ├─ /ErgoWallet          : Swift source code
│  ├─ /Tests               : Unit tests
│  ├─ project.pbxproj      : Xcode project file
│  └─ Podfile              : CocoaPods dependencies
│
├─ /desktop                 : Desktop wallet (Scala/Java)
│  ├─ /src/main/scala      : Scala source code
│  ├─ /src/main/resources  : Configuration files, FXML layouts
│  ├─ /src/test            : Unit tests
│  ├─ build.sbt            : SBT build configuration
│  └─ application.conf     : App configuration
│
├─ /common                  : Shared code across platforms
│  ├─ /src/commonMain      : Kotlin Multiplatform Common source
│  ├─ models               : Shared data models
│  └─ /src/commonTest      : Common tests
│
├─ /app                     : Web/React frontend (optional)
│  ├─ /src                 : React component source
│  ├─ package.json         : npm dependencies
│  └─ /public              : Static assets
│
├─ build.gradle            : Root Gradle build file
├─ build.sbt               : Root SBT build file
└─ settings.gradle         : Gradle modules and plugins

KEY FEATURES:
=============
1. Wallet Management
   ├─ Create new wallet (BIP39 mnemonic generation)
   ├─ Restore wallet from seed phrase
   ├─ Multi-wallet support (HD hierarchical deterministic)
   ├─ Watch-only wallets (public key only)
   └─ Cold wallet support (air-gapped for security)

2. Transaction Management
   ├─ Send ERG to addresses
   ├─ Send native tokens (assets)
   ├─ Multiple inputs/outputs support
   ├─ Fee estimation
   ├─ Transaction signing with private keys
   └─ Transaction history tracking

3. Security Features
   ├─ Seed phrase encryption
   ├─ Biometric authentication (fingerprint/face)
   ├─ Private key management
   ├─ QR code scanning (address receiving)
   ├─ QR code generation (cold wallet setup)
   └─ EIP3 standard compliance

4. dApp Integration
   ├─ ErgoPay protocol support
   ├─ Mosaik dApp UI framework
   ├─ Smart contract interaction
   ├─ Request signing and approval
   └─ dApp communication

5. Blockchain Integration
   ├─ Connect to Ergo node (REST API)
   ├─ Fetch wallet balances
   ├─ Monitor unconfirmed TXs
   ├─ Network switching (mainnet/testnet)
   └─ Blockchain state synchronization

TECHNOLOGY STACK:
=================
Mobile (Cross-Platform):
├─ Kotlin Multiplatform (shared logic)
├─ Android: Kotlin, Jetpack Compose or XML layouts
├─ iOS: Swift, UIKit or SwiftUI
├─ Local storage: SQLite, SharedPreferences, Keychain
└─ Crypto: libsodium, boringssl

Desktop:
├─ Scala (functional programming)
├─ Java (JVM 11+)
├─ JavaFX (GUI framework)
├─ FXML (layout files)
├─ Akka (async operations)
└─ PostgeSQL/H2 (local database)

Network & Crypto:
├─ REST APIs (Retrofit, OkHttp, Ktor)
├─ JSON serialization (Moshi, Kotlinx.serialization)
├─ Cryptography (Sigma protocols, EdDSA)
├─ BIP32/BIP39 (key derivation)
└─ QR code (ZXing, QR-native)

═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
ISSUE CATEGORIES & SOLUTIONS
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════

================================================================================
CATEGORY 1: WALLET CREATION & RESTORATION (Hard, 100 pts each)
================================================================================

### Task Type A: Wallet Creation Issues
When you see "Can't create wallet", "Mnemonic generation fails", "Wallet creation crashes":

1. Check wallet creation logic in /android or /ios or /desktop
   ├─ Find wallet generator/factory class
   ├─ Verify BIP39 mnemonic generation
   ├─ Check entropy generation (randomness quality)
   ├─ Verify key derivation (BIP32 path)

2. Common issues:
   ├─ Not using secure random source (entropy)
   ├─ Incorrect BIP32 derivation path
   ├─ Encoding issues (UTF-8 vs bytes)
   ├─ Checksum validation failing

3. Fix pattern (Kotlin):
   ```kotlin
   class WalletGenerator {
       fun createNewWallet(): Wallet {
           // Generate secure random entropy (128-256 bits)
           val entropy = SecureRandom().generateSeed(16) // 128 bits = 12 words
           
           // Convert entropy to BIP39 mnemonic
           val mnemonic = Bip39.fromEntropy(entropy)
           
           // Validate mnemonic (checksum)
           if (!Bip39.isValid(mnemonic)) {
               throw InvalidMnemonicException("Invalid mnemonic checksum")
           }
           
           // Derive master key from mnemonic
           val masterKey = Bip32.deriveMasterKey(mnemonic, passphrase = "")
           
           // Derive first address from m/44'/429'/0'/0/0
           val accountKey = masterKey.derive("m/44'/429'/0'/0/0")
           val address = accountKey.publicKey.toAddress()
           
           return Wallet(
               id = UUID.randomUUID().toString(),
               name = "My Wallet",
               mnemonic = mnemonic.encrypted(), // Store encrypted
               publicKey = accountKey.publicKey,
               address = address,
               createdAt = System.currentTimeMillis()
           )
       }
   }
   ```

4. Test:
   - Create wallet 10 times, verify each mnemonic is unique
   - Verify mnemonic is 12-24 words
   - Verify address generated matches standard
   - Import mnemonic in another wallet, verify address matches

### Task Type B: Wallet Restoration Issues
When you see "Restore fails", "Restored wallet empty", "Wrong address after restore":

1. Check restoration logic
   ├─ Mnemonic validation (BIP39)
   ├─ Passphrase handling (BIP39 optional)
   ├─ Key derivation path
   ├─ Address generation

2. Common issues:
   ├─ Not validating mnemonic before use
   ├─ Wrong derivation path (should be m/44'/429'/0'/0/*)
   ├─ Not deriving multiple addresses
   ├─ Encoding issues with seed phrase input

3. Fix pattern (Kotlin):
   ```kotlin
   fun restoreWalletFromMnemonic(
       mnemonic: String,
       passphrase: String = "",
       derivationIndices: IntRange = 0..0
   ): Wallet {
       // Validate mnemonic format
       val words = mnemonic.trim().split(Regex("\\s+"))
       if (words.size !in listOf(12, 15, 18, 21, 24)) {
           throw InvalidMnemonicException("Mnemonic must be 12, 15, 18, 21, or 24 words")
       }
       
       // Validate each word is in BIP39 wordlist
       if (!Bip39.isValid(mnemonic)) {
           throw InvalidMnemonicException("Invalid mnemonic - checksum failed")
       }
       
       // Derive master key with optional passphrase
       val masterKey = Bip32.deriveMasterKey(mnemonic, passphrase)
       
       // Derive addresses for multiple indices
       val addresses = mutableListOf<Address>()
       for (index in derivationIndices) {
           val path = "m/44'/429'/0'/0/$index"
           val key = masterKey.derive(path)
           addresses.add(key.publicKey.toAddress())
       }
       
       return Wallet(
           mnemonic = mnemonic.encrypted(),
           addresses = addresses,
           primaryAddress = addresses.first()
       )
   }
   ```

4. Test:
   - Get mnemonic from created wallet
   - Restore in new app instance
   - Verify address matches original
   - Verify multiple derivations work
   - Try with invalid mnemonic (should fail)

### Task Type C: Multi-Wallet Management
When you see "Can't switch wallets", "Wallet list empty", "Wallet selection fails":

1. Check wallet storage and retrieval
   ├─ Database queries for wallet list
   ├─ Wallet selection logic
   ├─ Wallet switching (updating active wallet)
   ├─ Wallet deletion

2. Common issues:
   ├─ Not persisting wallet list to database
   ├─ Not updating active wallet on switch
   ├─ Wallet data not cleared on logout
   ├─ Race condition in wallet selection

3. Fix pattern:
   ```kotlin
   class WalletRepository(private val db: WalletDatabase) {
       fun getAllWallets(): Flow<List<Wallet>> =
           db.walletDao().getAllWallets() // LiveData/Flow
       
       fun getActiveWallet(): Flow<Wallet?> =
           db.walletDao().getActiveWallet()
       
       suspend fun setActiveWallet(walletId: String) {
           // Clear previous active
           db.walletDao().clearActiveWallet()
           // Set new active
           db.walletDao().setActiveWallet(walletId)
       }
       
       suspend fun addWallet(wallet: Wallet) {
           db.walletDao().insert(wallet)
       }
       
       suspend fun deleteWallet(walletId: String) {
           db.walletDao().delete(walletId)
       }
   }
   ```

4. Test:
   - Create 3 wallets
   - Switch between them
   - Verify active wallet updates
   - Delete wallet, verify list updates

================================================================================
CATEGORY 2: TRANSACTION SENDING & SIGNING (Hard, 100 pts each)
================================================================================

### Task Type A: Transaction Creation Issues
When you see "Can't create TX", "Invalid inputs", "Output error", "Fee calculation wrong":

1. Check TX builder logic
   ├─ UTXO selection (coin selection algorithm)
   ├─ Input/output validation
   ├─ Fee estimation
   ├─ Change calculation

2. Common issues:
   ├─ Not selecting enough inputs for amount + fee
   ├─ Not creating change output (lost funds!)
   ├─ Fee estimation too low (TX stuck)
   ├─ Not handling multiple inputs/outputs

3. Fix pattern (Kotlin):
   ```kotlin
   class TransactionBuilder(private val walletService: WalletService) {
       fun buildSendTransaction(
           recipient: String,
           amount: Long,
           feeEstimate: Long? = null
       ): Transaction {
           val fee = feeEstimate ?: estimateFee(amount)
           val totalNeeded = amount + fee
           
           // Select UTXOs (coin selection)
           val unspentBoxes = walletService.getUnspentBoxes()
           val selectedBoxes = selectCoins(unspentBoxes, totalNeeded)
           
           if (selectedBoxes.isEmpty()) {
               throw InsufficientFundsException("Not enough balance")
           }
           
           val selectedSum = selectedBoxes.sumOf { it.value }
           val change = selectedSum - amount - fee
           
           // Create outputs
           val outputs = mutableListOf<Output>()
           
           // Add recipient output
           outputs.add(Output(
               address = recipient,
               value = amount,
               tokens = emptyList()
           ))
           
           // Add change output if needed
           if (change > 0) {
               outputs.add(Output(
                   address = walletService.getChangeAddress(),
                   value = change,
                   tokens = emptyList()
               ))
           }
           
           return Transaction(
               inputs = selectedBoxes.map { Input(it.boxId) },
               outputs = outputs,
               fee = fee,
               createdAt = System.currentTimeMillis()
           )
       }
       
       private fun selectCoins(boxes: List<Box>, amount: Long): List<Box> {
           // Simple selection: sort by value, pick largest first
           return boxes.sortedByDescending { it.value }
               .fold(emptyList<Box>() to 0L) { (selected, sum), box ->
                   if (sum >= amount) selected to sum
                   else (selected + box) to (sum + box.value)
               }.first
       }
       
       private fun estimateFee(amount: Long): Long {
           // Estimate: ~700 nanoERG per byte for 1 input/2 output TX
           // Typical TX size: 200-300 bytes
           return 100000 // 0.001 ERG minimum
       }
   }
   ```

4. Test:
   - Create TX with 10 ERG balance, send 5 ERG
   - Verify change output exists
   - Verify fee deducted
   - Try to send more than balance (should fail)
   - Verify TX structure is valid

### Task Type B: Transaction Signing Issues
When you see "Can't sign TX", "Signature invalid", "Proof generation fails":

1. Check signing logic
   ├─ Private key retrieval
   ├─ TX hash computation
   ├─ Sigma protocol proof generation
   ├─ Signature serialization

2. Common issues:
   ├─ Not unlocking wallet before signing (requires password/biometric)
   ├─ Using wrong key for signing
   ├─ Not including all TX data in signature
   ├─ Signature format incorrect

3. Fix pattern:
   ```kotlin
   class TransactionSigner(private val walletService: WalletService) {
       fun signTransaction(
           tx: Transaction,
           walletPassword: String? = null
       ): SignedTransaction {
           // Unlock wallet (verify password or biometric)
           walletService.unlockWallet(walletPassword)
           
           // Get private key for each input
           val signatures = mutableListOf<Signature>()
           for (input in tx.inputs) {
               // Find which address owns this input
               val address = walletService.findAddressForBox(input.boxId)
               val privateKey = walletService.getPrivateKey(address)
               
               // Compute transaction hash
               val txBytes = tx.toBytes()
               val txHash = Blake2b256.hash(txBytes)
               
               // Generate Sigma protocol proof
               val proof = SigmaProtocol.sign(
                   message = txHash,
                   privateKey = privateKey,
                   context = createProofContext(tx)
               )
               
               signatures.add(Signature(
                   inputIndex = tx.inputs.indexOf(input),
                   proof = proof
               ))
           }
           
           return SignedTransaction(
               transaction = tx,
               signatures = signatures,
               signedAt = System.currentTimeMillis()
           )
       }
       
       private fun createProofContext(tx: Transaction): ProofContext {
           return ProofContext(
               txId = tx.id,
               inputs = tx.inputs,
               outputs = tx.outputs,
               fee = tx.fee,
               blockHeight = getCurrentBlockHeight()
           )
       }
   }
   ```

4. Test:
   - Create and sign TX
   - Verify signature is valid
   - Modify TX after signing (should fail verification)
   - Sign with wrong key (should fail)

### Task Type C: Transaction Broadcasting Issues
When you see "TX broadcast fails", "Node rejects TX", "TX stuck in mempool":

1. Check broadcast logic
   ├─ Node endpoint connectivity
   ├─ TX serialization format
   ├─ HTTP request/response handling
   ├─ Error handling from node

2. Common issues:
   ├─ TX serialization format mismatch with node
   ├─ Node endpoint unreachable
   ├─ Invalid TX rejected by node validation
   ├─ Network timeout

3. Fix pattern:
   ```kotlin
   class TransactionBroadcaster(private val nodeClient: ErgoNodeClient) {
       suspend fun broadcastTransaction(signedTx: SignedTransaction): String {
           try {
               // Serialize TX to node format
               val txJson = signedTx.toJson()
               
               // Broadcast to node
               val response = nodeClient.submitTransaction(txJson)
               
               // Check response
               if (response.isSuccessful) {
                   val txId = response.body()?.txId
                   Logger.i("TX broadcast success: $txId")
                   return txId ?: throw BroadcastException("No TX ID in response")
               } else {
                   val errorMsg = response.errorBody()?.string()
                   Logger.e("Broadcast failed: $errorMsg")
                   throw BroadcastException(errorMsg ?: "Unknown error")
               }
           } catch (e: IOException) {
               Logger.e("Network error: ${e.message}")
               throw BroadcastException("Network error: ${e.message}")
           }
       }
   }
   ```

4. Test:
   - Broadcast TX to testnet node
   - Verify TX appears in mempool
   - Wait for confirmation (1 minute)
   - Verify TX in blockchain

================================================================================
CATEGORY 3: ADDRESS & BALANCE MANAGEMENT (Medium, 50 pts each)
================================================================================

### Task Type A: Address Derivation Issues
When you see "Address not found", "Wrong address generated", "Derivation fails":

1. Check address derivation logic
   ├─ BIP32 path validation
   ├─ Key derivation
   ├─ Address encoding (Base58)
   └─ Address type detection

2. Common issues:
   ├─ Hardcoded wrong derivation path
   ├─ Not deriving multiple addresses
   ├─ Address encoding wrong
   ├─ Not handling different address types

3. Fix pattern:
   ```kotlin
   fun deriveAddresses(masterKey: ExtendedPrivateKey, count: Int): List<Address> {
       val addresses = mutableListOf<Address>()
       for (i in 0 until count) {
           val derivationPath = "m/44'/429'/0'/0/$i"
           val key = masterKey.derive(derivationPath)
           val address = Address(
               path = derivationPath,
               publicKey = key.publicKey,
               address = key.publicKey.toErgoAddress(),
               index = i
           )
           addresses.add(address)
       }
       return addresses
   }
   ```

### Task Type B: Balance Fetching Issues
When you see "Balance not updating", "Shows zero balance", "Balance stuck":

1. Check balance sync logic
   ├─ Node API call to get UTXOs
   ├─ UTXO filtering
   ├─ Balance calculation
   ├─ Refresh timing

2. Common issues:
   ├─ Not refreshing balance on app start
   ├─ Not listening for blockchain updates
   ├─ Spent UTXOs not removed
   ├─ Race condition between fetch and display

3. Fix pattern:
   ```kotlin
   class BalanceManager(private val nodeClient: ErgoNodeClient) {
       suspend fun refreshBalance(address: String): Long {
           try {
               // Fetch unspent boxes for address
               val boxes = nodeClient.getUnspentBoxes(address)
               
               // Sum values
               val balance = boxes.sumOf { it.value }
               
               // Update UI
               balanceFlow.emit(balance)
               
               return balance
           } catch (e: Exception) {
               Logger.e("Balance fetch failed: ${e.message}")
               return 0L
           }
       }
   }
   ```

================================================================================
CATEGORY 4: BLOCKCHAIN INTEGRATION (Medium, 50 pts each)
================================================================================

### Task Type A: Node Connection Issues
When you see "Can't connect to node", "Node unreachable", "Network error":

1. Check node client configuration
   ├─ Node URL in config
   ├─ Network switching (mainnet/testnet)
   ├─ Connection timeout settings
   ├─ Retry logic

2. Common issues:
   ├─ Hardcoded node URL not working
   ├─ Not switching node URL on network change
   ├─ Timeout too short
   ├─ No retry on transient failure

3. Fix pattern:
   ```kotlin
   class ErgoNodeClient(
       private val okHttpClient: OkHttpClient,
       private val baseUrl: String
   ) {
       companion object {
           // Mainnet and testnet nodes
           const val MAINNET_URL = "https://mainnet.ergoplatform.com"
           const val TESTNET_URL = "http://testnet.ergoplatform.com"
       }
       
       suspend fun getBalance(address: String): Long = withRetry {
           val response = okHttpClient.newCall(
               Request.Builder()
                   .url("$baseUrl/api/v1/addresses/$address")
                   .get()
                   .build()
           ).execute()
           
           return response.body?.string()?.let {
               Json.decodeFromString<AddressInfo>(it).balance
           } ?: 0L
       }
       
       private suspend inline fun <T> withRetry(
           maxRetries: Int = 3,
           initialDelay: Long = 1000,
           block: suspend () -> T
       ): T {
           var attempt = 0
           var lastException: Exception? = null
           
           while (attempt < maxRetries) {
               try {
                   return block()
               } catch (e: Exception) {
                   lastException = e
                   attempt++
                   if (attempt < maxRetries) {
                       delay(initialDelay * attempt)
                   }
               }
           }
           
           throw lastException ?: Exception("Max retries exceeded")
       }
   }
   ```

================================================================================
CATEGORY 5: SECURITY & ENCRYPTION (Hard, 100 pts each)
================================================================================

### Task Type A: Seed Phrase Encryption Issues
When you see "Can't encrypt seed", "Decryption fails", "Seed exposed in logs":

1. Check encryption logic
   ├─ Encryption algorithm (AES-256)
   ├─ Key derivation (PBKDF2)
   ├─ IV generation (randomness)
   ├─ Secure storage (Keystore/Keychain)

2. Common issues:
   ├─ Using weak encryption
   ├─ Not using IV or using fixed IV
   ├─ Not deriving key properly from password
   ├─ Storing encrypted seed insecurely
   ├─ Logging seed phrase in debug logs

3. Fix pattern:
   ```kotlin
   class EncryptionService {
       fun encryptSeedPhrase(
           seedPhrase: String,
           password: String
       ): EncryptedSeed {
           // Derive encryption key from password
           val salt = SecureRandom().generateSeed(16)
           val key = PBKDF2.deriveKey(
               password = password,
               salt = salt,
               iterations = 10000,
               keyLength = 256
           )
           
           // Generate IV
           val iv = SecureRandom().generateSeed(16)
           
           // Encrypt seed phrase
           val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
           cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
           val encryptedBytes = cipher.doFinal(seedPhrase.toByteArray(Charsets.UTF_8))
           
           return EncryptedSeed(
               encryptedData = Base64.encode(encryptedBytes),
               salt = Base64.encode(salt),
               iv = Base64.encode(iv),
               algorithm = "AES-256-CBC"
           )
       }
       
       fun decryptSeedPhrase(
           encrypted: EncryptedSeed,
           password: String
       ): String {
           // Derive same key from password and salt
           val salt = Base64.decode(encrypted.salt)
           val key = PBKDF2.deriveKey(
               password = password,
               salt = salt,
               iterations = 10000,
               keyLength = 256
           )
           
           // Decrypt
           val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
           val iv = Base64.decode(encrypted.iv)
           cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
           val decryptedBytes = cipher.doFinal(Base64.decode(encrypted.encryptedData))
           
           return String(decryptedBytes, Charsets.UTF_8)
       }
   }
   ```

### Task Type B: Biometric Authentication Issues
When you see "Fingerprint not working", "Face ID fails", "Auth crashes":

1. Check biometric integration
   ├─ BiometricPrompt initialization
   ├─ Cipher setup
   ├─ Authentication flow
   ├─ Error handling

2. Common issues:
   ├─ Not checking if biometric is available
   ├─ Not handling authentication failure
   ├─ Cipher not properly initialized
   ├─ Not storing encrypted data for biometric unlock

3. Fix pattern (Android):
   ```kotlin
   class BiometricAuthManager(private val context: Context) {
       fun isBiometricAvailable(): Boolean {
           val biometricManager = BiometricManager.from(context)
           return biometricManager.canAuthenticate(
               BiometricManager.Authenticators.BIOMETRIC_STRONG
           ) == BiometricManager.BIOMETRIC_SUCCESS
       }
       
       fun showBiometricPrompt(
           fragmentActivity: FragmentActivity,
           onSuccess: (String) -> Unit,
           onError: (String) -> Unit
       ) {
           val promptInfo = BiometricPrompt.PromptInfo.Builder()
               .setTitle("Unlock Wallet")
               .setSubtitle("Use your biometric to unlock")
               .setNegativeButtonText("Cancel")
               .build()
           
           val biometricPrompt = BiometricPrompt(
               fragmentActivity,
               object : BiometricPrompt.AuthenticationCallback() {
                   override fun onAuthenticationSucceeded(
                       result: BiometricPrompt.AuthenticationResult
                   ) {
                       super.onAuthenticationSucceeded(result)
                       onSuccess("Authenticated")
                   }
                   
                   override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                       super.onAuthenticationError(errorCode, errString)
                       onError("Authentication failed: $errString")
                   }
               }
           )
           
           biometricPrompt.authenticate(promptInfo)
       }
   }
   ```

================================================================================
CATEGORY 6: USER INTERFACE & UX (Medium, 50 pts each)
================================================================================

### Task Type A: Screen Layout Issues
When you see "UI broken on small screens", "Text overflow", "Buttons unclickable":

1. Check layout files
   ├─ Responsive design
   ├─ Touch targets (min 48dp)
   ├─ Text overflow handling
   ├─ Safe areas

2. Common issues:
   ├─ Hardcoded sizes not responsive
   ├─ Text not wrapping
   ├─ Buttons too small
   ├─ Ignoring safe area (notches, etc)

3. Fix pattern (Android Compose):
   ```kotlin
   @Composable
   fun SendScreen() {
       Column(
           modifier = Modifier
               .fillMaxSize()
               .padding(16.dp)
               .systemBarsPadding() // Handle safe area
               .verticalScroll(rememberScrollState()),
           verticalArrangement = Arrangement.spacedBy(16.dp)
       ) {
           TextField(
               value = recipientAddress,
               onValueChange = { },
               modifier = Modifier
                   .fillMaxWidth()
                   .heightIn(min = 48.dp), // Min touch target
               label = { Text("Recipient Address") }
           )
           
           Button(
               onClick = { },
               modifier = Modifier
                   .fillMaxWidth()
                   .heightIn(min = 48.dp) // Min touch target
           ) {
               Text("Send")
           }
       }
   }
   ```

### Task Type B: Data Display Issues
When you see "Balance not showing", "TX list empty", "Prices wrong format":

1. Check data binding/display
   ├─ Data fetching
   ├─ Formatting (decimals, units)
   ├─ List rendering
   ├─ Error states

2. Common issues:
   ├─ Not fetching data on screen load
   ├─ Not formatting numbers properly
   ├─ Not handling empty states
   ├─ Not updating UI on data change

3. Fix pattern:
   ```kotlin
   @Composable
   fun BalanceDisplay(viewModel: WalletViewModel) {
       val balance by viewModel.balance.collectAsState()
       
       LaunchedEffect(Unit) {
           viewModel.refreshBalance() // Fetch on load
       }
       
       when (balance) {
           is Loading -> LoadingSpinner()
           is Success -> {
               val value = (balance as Success).data
               Text(
                   text = formatErgo(value), // Format: "1.234567 ERG"
                   style = MaterialTheme.typography.headlineMedium
               )
           }
           is Error -> ErrorMessage((balance as Error).message)
       }
   }
   ```

================================================================================
CATEGORY 7: TESTING & CODE QUALITY (Medium, 50 pts each)
================================================================================

### Task Type A: Unit Test Issues
When you see "Tests failing", "Low coverage", "Test doesn't work":

1. Check test structure
   ├─ Mock dependencies
   ├─ Test data
   ├─ Assertions
   ├─ Async handling

2. Common issues:
   ├─ Not mocking network calls
   ├─ Async operations not awaited
   ├─ Tests dependent on external state
   ├─ No test data fixtures

3. Fix pattern (Kotlin):
   ```kotlin
   class TransactionSignerTest {
       private val mockWalletService = mockk<WalletService>()
       private val signer = TransactionSigner(mockWalletService)
       
       @Test
       fun `sign transaction creates valid signature`() = runBlocking {
           // Arrange
           val testTx = createTestTransaction()
           coEvery { mockWalletService.unlockWallet(any()) } returns Unit
           coEvery { mockWalletService.getPrivateKey(any()) } returns testPrivateKey
           
           // Act
           val signedTx = signer.signTransaction(testTx, "password")
           
           // Assert
           assertNotNull(signedTx.signatures)
           assertEquals(testTx.inputs.size, signedTx.signatures.size)
       }
   }
   ```

================================================================================
COMMON ISSUE PATTERNS & SOLUTIONS
================================================================================

PATTERN 1: "Wallet won't unlock"
──────────────────────────────
Symptoms: "Wrong password", "Biometric fails", "Can't access funds"

Solutions:
1. Verify password/passphrase is correct
2. Check if biometric is properly enrolled on device
3. Clear app cache and try again
4. Verify wallet file not corrupted

PATTERN 2: "TX stuck in mempool"
────────────────────────────────
Symptoms: "TX pending for hours", "Never confirms", "Still unconfirmed"

Solutions:
1. Check if fee is too low (network congestion)
2. Verify node is synced (up-to-date)
3. Bump fee (create new TX with higher fee)
4. Check if node rejected TX (verify signature)

PATTERN 3: "Balance shows wrong amount"
──────────────────────────────────────
Symptoms: "Shows zero when should have funds", "Amount decreased", "Includes spent UTXOs"

Solutions:
1. Refresh balance manually
2. Check if address is correct
3. Derive more addresses (might have multiple)
4. Wait for blockchain sync
5. Verify UTXOs not double-counted

PATTERN 4: "Can't send to address"
──────────────────────────────────
Symptoms: "Invalid address error", "Address rejected", "Recipient not found"

Solutions:
1. Verify address format (Base58, correct length)
2. Check address is for same network (testnet vs mainnet)
3. Verify recipient address is valid Ergo address
4. Check address doesn't contain typos

PATTERN 5: "App crashes on startup"
──────────────────────────────────
Symptoms: "Force close", "NullPointerException", "Crash loop"

Solutions:
1. Clear app cache and data
2. Reinstall app
3. Check device storage (low space?)
4. Check Android/iOS version compatibility
5. Try on another device

═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════
GENERAL WORKFLOW FOR EACH ISSUE
═══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════════

1. **Read Issue Carefully**
   - Understand what's broken
   - Identify platform (Android/iOS/Desktop)
   - Look for error messages/stack traces
   - Check reproduction steps

2. **Set Up Development**
   ```bash
   git clone https://github.com/YOUR_FORK/ergo-wallet-app
   cd ergo-wallet-app
   
   # For Android
   ./gradlew build
   ./gradlew test
   
   # For iOS
   cd ios && pod install
   
   # For Desktop
   cd desktop && sbt compile && sbt test
   ```

3. **Reproduce the Issue**
   - Follow steps from issue description
   - Check logs for errors
   - Use debugger to inspect state
   - Try on different devices/OS versions

4. **Locate the Code**
   - Find relevant file (Android/iOS/Desktop)
   - Search for related classes/functions
   - Read code path from start to issue
   - Understand current implementation

5. **Fix the Issue**
   - Keep changes focused on issue
   - Follow platform conventions
   - Handle edge cases
   - Add logging for debugging

6. **Test the Fix**
   - Unit tests: ./gradlew test (Android) or sbt test (Desktop)
   - Manual testing: create wallet, send TX, etc
   - Test on multiple devices/versions
   - Verify no regressions

7. **Check Code Quality**
   - Lint: ./gradlew lint (Android)
   - Format: ktlint or swiftlint
   - Type safety: verify no casting

8. **Create Clean Commit**
   ```bash
   git checkout -b fix/issue-name
   git add .
   git commit -m "Fix: [issue title] (#issue-number)"
   ```

9. **Push and Create PR**
   ```bash
   git push origin fix/issue-name
   # Create PR with description of fix
   ```

