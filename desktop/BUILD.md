## Build yourself

### Run from command line
* Clone this repo 
* Run `./gradlew desktop:run` or `./gradlew desktop:run --args="(command line arguments)"`
* If you have problems regarding Android SDK, deactivate android inclusion in `settings.gradle` file

### Build for current system/arch
* Fat jar: `./gradlew desktop:shadowJar`
* Minified jar: `./gradlew desktop:build`

### Build for other system/arch
* Fat jar: `./gradlew desktop:shadowJar -Posarch=macos-x64`

Supported os/arch:
linux-x64, linux-arm64, windows-x64, macos-x64, macos-arm64

### Command line arguments
* `--testnet` will run for testnet