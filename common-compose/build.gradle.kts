plugins {
    id("kotlin")
    id("org.jetbrains.compose") version "1.6.11"
}

val mosaik_version: String by rootProject.extra

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(":common-jvm"))

    api(compose.runtime)
    api(compose.foundation)
    api(compose.material)
    api("com.github.MrStahlfelge.mosaik:common-compose:$mosaik_version")
    api(compose.materialIconsExtended)
    // Needed only for preview.
    implementation(compose.preview)
}
