val kotlin_version: String by project
val serialization_version: String by project
val kjunit_version: String by project
val junit_version: String by project

plugins {
    kotlin("plugin.serialization") version "1.7.10"
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kjunit_version")
    testImplementation("junit:junit:$junit_version")
}

description = "Kotlin library providing extra Kotlin serializers"
version = "3.0.0"
