plugins {
    id("kotlin")
    id("org.jetbrains.compose") version "1.1.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":common-jvm"))
    implementation(project(":sqldelight"))
    implementation("com.squareup.sqldelight:sqlite-driver:1.5.3")

    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("com.github.MrStahlfelge.mosaik:common-compose:e8b73e22c7")

    implementation("com.arkivanov.decompose:decompose:0.5.2")
    implementation("com.arkivanov.decompose:extensions-compose-jetbrains:0.5.2")
    implementation("net.harawata:appdirs:1.2.1") // https://github.com/harawata/appdirs

    // https://levelup.gitconnected.com/qr-code-scanner-in-kotlin-e15dd9bfbb1f
    arrayOf("core","kotlin","WebcamCapture").forEach()
    { implementation("org.boofcv:boofcv-$it:0.40.1") {
        exclude("org.boofcv", "boofcv-swing")
    } }
}

compose.desktop {
    application {
        mainClass = "org.ergoplatform.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "Ergo Wallet App"
        }
    }
}

tasks {
    processResources {
        doFirst {
            copy {
                from("../ios/resources/i18n")
                into("src/main/resources/i18n")
                include("*.properties")
            }
        }
    }
}
