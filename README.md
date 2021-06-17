# Ergo Wallet Android

Ergo Wallet for Android, built on top of [Ergo Appkit](https://github.com/aslesarenko/ergo-appkit).

This is work in progress, and use at your own risk. See releases section for binaries (built on GitHub) and release notes.

Visit the [Ergo Discord](https://discord.gg/kj7s7nb) for more information.


# TODO
- [X] Dark mode setting in settings
- [X] Prevent same wallet being saved twice
- [X] Biometric Auth Security https://stackoverflow.com/a/62445439/7487013
- [ ] Progress indicator when restoring wallet (slow on some devices)
- [ ] Generate new wallets
- [ ] Export secrets/mnemonic
- [ ] Prevent sending transaction when there are unconfirmed transactions (does not work)
- [ ] Gradle verify dependencies
- [ ] API < 26 for all: https://github.com/ergoplatform/ergo-appkit/issues/82
- [ ] Change to appkit's secret storage https://github.com/ergoplatform/ergo-appkit/issues/84

And further more
- [ ] Derive addresses from master key
- [ ] Address book
- [ ] In app transactions list and details