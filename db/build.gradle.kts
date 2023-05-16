val serialization_version: String by project
val slf4j_version: String by project

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
}

dependencies {
    implementation("org.slf4j:slf4j-api:$slf4j_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")
}