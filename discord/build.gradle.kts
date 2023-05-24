plugins {
    application
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
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
    implementation(project(":api-client-kt"))
    implementation(libs.jda)
    implementation(libs.jda.ktx)
    implementation(libs.configurate.yaml)
    implementation(libs.configurate.extra.kotlin)
    implementation(libs.logback)
}

description = "Discord bot for managing the GDQ schedule channel"
version = "3.0.0"
