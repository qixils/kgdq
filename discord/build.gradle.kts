val configurate_version: String by project

plugins {
    application
    kotlin("plugin.serialization") version "1.7.10"
}

application {
    mainClass.set("dev.qixils.discord.BotKt")
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
    implementation(project(":api"))
    implementation("net.dv8tion:JDA:5.0.0-alpha.17")
    implementation("com.github.minndevelopment:jda-ktx:03b07e7d17")
    implementation("org.spongepowered:configurate-yaml:$configurate_version")
    implementation("org.spongepowered:configurate-extra-kotlin:$configurate_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.7.10")
    testImplementation("junit:junit:4.13.2")
}

description = "Discord bot for managing the GDQ schedule channel"
version = "3.0.0"
