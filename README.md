# Ergo Wallet Android

Ergo Wallet for Android, built on top of [Ergo Appkit](https://github.com/aslesarenko/ergo-appkit).

This is work in progress, and use at your own risk. See releases section for binaries (built on GitHub) and release notes.

Visit the [Ergo Discord](https://discord.gg/kj7s7nb) for more information.


# TODO
- [X] CoinGecko Fiat value request
- [X] Edit/Delete for Wallets
- [ ] Configurable fiat currency, add link to CoinGecko to about screen
- [ ] Biometric Auth Security https://stackoverflow.com/a/62445439/7487013
- [ ] Export secrets/mnemonic
- [ ] Show error reasons for sending transaction (no unspent boxes)
- [ ] Prevent sending transaction when there are unconfirmed transactions (does not work)
- [ ] Generate new wallets when https://github.com/ergoplatform/ergo-appkit/issues/87 is released
- [ ] Prevent the same wallet being saved twice
- [ ] Amount input filter https://stackoverflow.com/a/13716371/7487013
- [ ] Gradle verify dependencies
- [ ] API < 26 for all: https://github.com/ergoplatform/ergo-appkit/issues/82
- [ ] Change to appkit's secret storage https://github.com/ergoplatform/ergo-appkit/issues/84

And further more
- [ ] Address book
- [ ] In app transactions list and details