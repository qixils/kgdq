plugins {
    application
    kotlin("plugin.serialization") version "1.7.10"
}

dependencies {
    api(project(":serializers"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")
    testImplementation("junit:junit:4.13.2")
}

description = "Kotlin library for the Horaro API"
version = "3.0.0"
