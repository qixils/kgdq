val serialization_version: String by project

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")
}