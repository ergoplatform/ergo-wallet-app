plugins {
    id("kotlin")
    id("org.jetbrains.compose") version "1.1.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(project(":common-jvm"))

    api(compose.runtime)
    api(compose.foundation)
    api(compose.material)
    api("com.github.MrStahlfelge.mosaik:common-compose:24fc4fe3d6")
    implementation(compose.materialIconsExtended)
    // Needed only for preview.
    implementation(compose.preview)
}
