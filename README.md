# Ergo Wallet Android

Ergo Wallet for Android, built on top of [Ergo Appkit](https://github.com/aslesarenko/ergo-appkit).

<img src="https://user-images.githubusercontent.com/26038055/122617266-38b02980-d08c-11eb-8cd7-a33d9984d002.png" width="250">

You need at least Android 7 to run Ergo Wallet.

Features:
* generating wallets, restoring wallets in a way compatible to Yoroi and Ergo node
* no need to make a full sync, this is a lightweight client
* Requesting payments by showing QR code or sharing a link
* adding read-only wallets to watch balance
* Show wallet balance, configurable comparison fiat currency
* Sending payments

Current state: Ready for testing. You can test the app on testnet. Generate a new wallet and send
yourself some test Ergos by visiting https://faucet.ergopool.io/payment/address/TESTNET_WALLET_ADDRESS

See releases section for binaries (built on GitHub) and release notes. It is normal that Google Play Protect warns about
an unsafe app. GitHub builds the binaries with a certificate unknown to Google.
Visit the [Ergo Discord](https://discord.gg/kj7s7nb) to give feedback.

## What's to come

### for first stable
- [ ] Progress indicator when restoring wallet (slow on some devices)
- [X] Gradle verify dependencies

### Extend core functionality
- [ ] Prevent sending transaction when there are unconfirmed transactions (transaction is discarded)
- [ ] Check if assets are safe, filter unspent boxes
- [ ] Support P2S addresses
- [ ] API < 26 for all: https://github.com/ergoplatform/ergo-appkit/issues/82

### And maybe in the future
- [ ] Derive further addresses from master key
- [ ] Address book
- [ ] In app transactions list and details
