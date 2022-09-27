val jraw_version: String by project
val okhttp_version: String by project
val configurate_version: String by project
val coroutines_version: String by project
val serialization_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClass.set("dev.qixils.gdq.reddit.ThreadMaster")
}

repositories {
    maven("https://jitpack.io") {
        name = "jitpack"
        content {
            includeGroup("com.github.dukestreet.JRAW")
        }
    }
}

dependencies {
    implementation(project(":api-models"))
    implementation("com.github.dukestreet.JRAW:lib:$jraw_version")
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    implementation("org.spongepowered:configurate-yaml:$configurate_version")
    implementation("org.spongepowered:configurate-extra-kotlin:$configurate_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
}

description = "Reddit bot for managing the GDQ VOD thread"
version = "3.0.0"