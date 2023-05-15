val jda_version: String by project
val jda_ktx_version: String by project
val configurate_version: String by project
val coroutines_version: String by project
val serialization_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("plugin.serialization") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClass.set("dev.qixils.gdq.discord.Bot")
}

repositories {
    maven("https://jitpack.io") {
        name = "jitpack"
        content {
            includeGroup("com.github.minndevelopment")
        }
    }
}

dependencies {
    implementation(project(":api-models"))
    implementation("net.dv8tion:JDA:$jda_version")
    implementation("com.github.minndevelopment:jda-ktx:$jda_ktx_version")
    implementation("org.spongepowered:configurate-yaml:$configurate_version")
    implementation("org.spongepowered:configurate-extra-kotlin:$configurate_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
}

description = "Discord bot for managing the GDQ schedule channel"
version = "3.0.0"
