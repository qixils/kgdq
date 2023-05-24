plugins {
    application
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
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
    implementation(project(":api-client-kotlin"))
    implementation(libs.jraw)
    implementation(libs.okhttp)
    implementation(libs.configurate.yaml)
    implementation(libs.configurate.extra.kotlin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback)
}

description = "Reddit bot for managing the GDQ VOD thread"
version = "3.0.0"