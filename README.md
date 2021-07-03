# Ergo Wallet Android

Ergo Wallet for Android, built on top of [Ergo Appkit](https://github.com/aslesarenko/ergo-appkit).

<img src="https://user-images.githubusercontent.com/26038055/122617266-38b02980-d08c-11eb-8cd7-a33d9984d002.png" width="250">

You need at least Android 7 to run Ergo Wallet.

Features:
* generating wallets, restoring wallets in a way compatible to Yoroi and Ergo node
* no need to make a full sync, this is a lightweight client
* Requesting payments by showing QR code or sharing a link
* Sending payments, manually or by scanning a QR code
* Your secrets are stored password-encrypted or authentication-protected
* adding read-only wallets to watch balance
* Show wallet balance, configurable comparison fiat currency

Current state: Ready for testing. You can test the app on testnet. Generate a new wallet and send
yourself some test Ergos by visiting https://faucet.ergopool.io/payment/address/TESTNET_WALLET_ADDRESS

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

## What's to come

### for first stable
- [ ] Progress indicator when restoring wallet (slow on some devices)
- [ ] Password dialog with a second confirmation text field when setting up a wallet

### Extend core functionality
- [ ] Prevent sending transaction when there are unconfirmed transactions (transaction is discarded)
- [X] Check if assets are safe -> done, they are
- [ ] Support P2S addresses
- [ ] switch to Explorer API v1

### And maybe in the future
- [ ] Derive further addresses from master key
- [ ] Address book
- [ ] In app transactions list and details
- [ ] Notifications for new transactions
