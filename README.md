# Ergo Wallet Android

Ergo Wallet for Android, built on top of [Ergo Appkit](https://github.com/aslesarenko/ergo-appkit).

<img src="https://user-images.githubusercontent.com/26038055/122617266-38b02980-d08c-11eb-8cd7-a33d9984d002.png" width="250">

You need at least Android 7 to run Ergo Wallet.

Features:
* generating wallets, restoring wallets in a way compatible to Yoroi and Ergo node (only first address can be seen at the moment)
* you can add read-only wallets without entering your secrets to watch balance
* no need to make a full sync, this is a lightweight client
* Requesting payments by showing QR code or sharing a link
* Sending payments, manually or by scanning a QR code
* Your secrets are stored password-encrypted or authentication-protected
* Show wallet balance, configurable comparison fiat currency

Current state: We have a first stable version to use. If you want, you can test the app on testnet. Generate a new wallet and send
yourself some test Ergos by visiting https://faucet.ergopool.io/payment/address/TESTNET_WALLET_ADDRESS
(replace TESTNET_WALLET_ADDRESS with your actual P2PK address)

Visit the [Ergo Discord](https://discord.gg/kj7s7nb) to give feedback.

### Download and install
You can download the app APKs from the [releases section](https://github.com/MrStahlfelge/ergo-wallet-android/releases).
There are APKs available for Testnet and Mainnet, and as a debug build and release build.

**Debug builds** are built on GitHub.
It is normal that Google Play Protect warns about
an unsafe app. GitHub builds the binaries with a certificate unknown to Google.
Because the certificate changes, you can't upgrade the app later - you need to uninstall and install fresh.

**Release builds** are built by me with my developer certificate and minified. This should reduce
Google Play Protect warnings, you'll be able to upgrade without losing your data and the app is
much smaller and faster, however, you have to trust me.

The APK file can be installed on your Android device. If you do this for the first time,
[you can follow this guide](https://www.xda-developers.com/sideload-apps-how-to/).

### Can I trust this app?

The app was [audited by the Ergo team](https://twitter.com/ergoplatformorg/status/1417039106576633857). 
If you still don't trust this app, you can add your wallets without giving your mnemonic. You won't be able to send funds then, but you can watch your balance.

### Build yourself

#### Deploy to your phone with Android Studio
* Download Android Studio
* Clone this repo with "New project from version control"
* Let Android Studio download all necessary stuff
* Enable developer mode on your phone, connect it and hit the play button in Android Studio

#### Create the APK
* Clone this repo
* Download and install Android SDK (not necessary when you installed Android Studio)
* Set up OpenJDK8 (not necessary when you installed Android Studio)
* Run `./gradlew assembleDebug`

### Tip the developer

If you want to tip the developer for making this app, thanks in advance! Send your tips to
[9ewA9T53dy5qvAkcR5jVCtbaDW2XgWzbLPs5H4uCJJavmA4fzDx](https://explorer.ergoplatform.com/payment-request?address=9ewA9T53dy5qvAkcR5jVCtbaDW2XgWzbLPs5H4uCJJavmA4fzDx&amount=0&description=)

## What's to come

### Extend core functionality
- [ ] Prevent sending transaction when there are unconfirmed transactions (transaction is discarded)
- [ ] Filter unspent boxes with assets
- [ ] Support P2S addresses
- [ ] switch to Explorer API v1
- [ ] Derive further addresses from master key

### And maybe in the future
- [ ] Address book
- [ ] In app transactions list and details
- [ ] Notifications for new transactions
