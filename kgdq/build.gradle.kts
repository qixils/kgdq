plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":serializers"))
    implementation(libs.slf4j)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}

description = "Kotlin library for the GDQ Donation Tracker API"
version = "3.0.0"
