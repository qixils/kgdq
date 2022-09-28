val slf4j_version: String by project
val coroutines_version: String by project
val serialization_version: String by project
val junit_version: String by project
val kjunit_version: String by project

plugins {
    kotlin("plugin.serialization") version "1.7.10"
}

dependencies {
    api(project(":serializers"))
    implementation("org.slf4j:slf4j-api:$slf4j_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kjunit_version")
    testImplementation("junit:junit:$junit_version")
}

description = "Kotlin library for the Speedrun.com API"
version = "1.0.0"