# Ergo Wallet App

<img src="https://user-images.githubusercontent.com/26038055/131368542-0e401c2c-35e4-449c-8423-ea259b39614b.png" align="right"  width="250">

Official Ergo Wallet App ([official announcement](https://ergoplatform.org/en/blog/2021-07-29-ergo-for-android-released/))

<a href="https://play.google.com/store/apps/details?id=org.ergoplatform.android"><img alt="Get it on Google Play" src="https://user-images.githubusercontent.com/11427267/75923897-483f3b00-5e66-11ea-8ec7-e86887afea51.png"></a>
<a href="https://apps.apple.com/app/terminus-wallet-ergo/id1643137927"><img alt="Download App Store" src="https://user-images.githubusercontent.com/11427267/75923896-47a6a480-5e66-11ea-87c1-3ec73ebcf7a5.png"></a>

Features:
* generating wallets, restoring wallets in a way compatible to Yoroi and Ergo node
* you can add read-only wallets without entering your secrets to watch balance or to prepare transactions for [cold wallet devices](https://github.com/ergoplatform/ergo-wallet-app/wiki/Cold-wallet)
* no need to make a full sync, this is a lightweight client
* Requesting payments by showing QR code or sharing a link
* Sending payments, manually or by scanning a QR code
* Displays and sends tokens and NFT
* Your secrets are stored password-encrypted or authentication-protected
* Show wallet balance, configurable comparison fiat currency
* Cold wallet capable ([more information](https://github.com/ergoplatform/ergo-wallet-app/wiki/Cold-wallet))
* ErgoPay support
* ErgoAuth support

You need at least Android 7 or iOS 13 to run Ergo Wallet.

On Linux and MacOS, you need Java 11 or 17 to run Ergo Wallet. [More information](desktop/RUN.md)

Visit the [Ergo Discord](https://discord.gg/kj7s7nb) to give feedback.

### Download and install the APK manually

Apart from Google Play, you can download the app APKs from the [releases section](https://github.com/ergoplatform/ergo-wallet-app/releases) to sideload.
There are APKs available for Testnet and Mainnet, and as a debug build and release build.

**Debug builds** are built on GitHub.
It is normal that Google Play Protect warns about
an unsafe app. GitHub builds the binaries with a certificate unknown to Google.
Because the certificate changes, you can't upgrade the app later - you need to uninstall and install fresh.

**Release builds** are built by me with my developer certificate and minified. This should reduce
Google Play Protect warnings, you'll be able to upgrade without losing your data and the app is
much smaller and faster, however, you have to trust me.

The APK file can be installed on your Android device. If you sideload for the first time,
[you can follow this guide](https://www.xda-developers.com/sideload-apps-how-to/).

### Build yourself

* [Android](android/BUILD.md)
* [iOS](ios/BUILD.md)
* [Desktop](desktop/BUILD.md)

### Translations

Every translation is welcome! There is a single 
[strings file to translate](https://github.com/ergoplatform/ergo-wallet-app/blob/develop/android/src/main/res/values/strings.xml) 
to your language.

Either send me the translated file on Discord or Telegram, or open a PR here. For this, move the 
file to a values-xx directory where xx is your language's ISO code. 
([Spanish example](https://github.com/ergoplatform/ergo-wallet-app/tree/develop/android/src/main/res/values-es))

Thanks in advance!

### Tip the developer

If you want to tip the developer for making this app, thanks in advance! Send your tips to
[9ewA9T53dy5qvAkcR5jVCtbaDW2XgWzbLPs5H4uCJJavmA4fzDx](https://explorer.ergoplatform.com/payment-request?address=9ewA9T53dy5qvAkcR5jVCtbaDW2XgWzbLPs5H4uCJJavmA4fzDx&amount=0&description=)

### Testing on Testnet
You can test the testnet Android debug build on testnet or build the iOS version yourself for testnet. Generate a new wallet and send
yourself some test Ergos by visiting https://testnet.ergofaucet.org/

