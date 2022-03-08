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
}

compose.desktop {
    application {
        mainClass = "org.ergoplatform.MainKt"
    }
}