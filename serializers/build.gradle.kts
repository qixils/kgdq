plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlin.stdlib.jdk8)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}

description = "Kotlin library providing extra Kotlin serializers"
version = "3.0.0"
