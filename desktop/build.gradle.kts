plugins {
    id("kotlin")
    id("org.jetbrains.compose") version "1.1.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":common-jvm"))
    implementation(project(":sqldelight"))

    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)

    implementation("com.arkivanov.decompose:decompose:0.5.2")
    implementation("com.arkivanov.decompose:extensions-compose-jetbrains:0.5.2")
}

compose.desktop {
    application {
        mainClass = "org.ergoplatform.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "Ergo Wallet App"
        }
    }
}