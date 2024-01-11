plugins {
    application
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("dev.qixils.gdq.discord.Bot")
}

repositories {
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
