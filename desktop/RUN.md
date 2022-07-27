## Use Ergo Wallet App on desktop

Ergo Wallet app releases contain jar artifacts to be used on the common desktop operating systems.

Download and run them with Java 11 or Java 17:

`java -jar <jar name> <optional command line arguments>`

For Windows, there is also an msi setup package provided - you don't need to install Java.

### Command line arguments
* `--testnet` runs testnet version
* `--debug` prints out debug information
* `ergopay://...` performs ErgoPay singing request
* `ergo:..` performs ergo payment request
* `ergoauth://...` performs ErgoAuth action