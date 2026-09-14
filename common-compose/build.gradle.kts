plugins {
    id("kotlin")
    id("org.jetbrains.compose") version "1.3.1"
}

val mosaik_version: String by rootProject.extra

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
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
