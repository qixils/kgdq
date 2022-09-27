val serialization_version: String by project

plugins {
    application
    kotlin("plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClass.set("dev.qixils.gdq.reddit.ThreadManager")
}

dependencies {
    implementation(project(":api-models"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
}

description = "Reddit bot for managing the GDQ VOD thread"
version = "3.0.0"