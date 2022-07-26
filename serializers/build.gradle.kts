val kotlin_version: String by project

plugins {
    kotlin("plugin.serialization") version "1.7.10"
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.3")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")
    testImplementation("junit:junit:4.13.2")
}

description = "Kotlin library providing extra Kotlin serializers"
version = "3.0.0"
