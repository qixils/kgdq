val serialization_version: String by project

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")
}