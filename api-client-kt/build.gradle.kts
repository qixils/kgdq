plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":api-models"))
    api(project(":kgdq")) // TODO: remove?
    api(libs.slf4j)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.bundles.ktor.client)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}

description = "Kotlin library for the vods.speedrun.club API"
version = "1.0.0"