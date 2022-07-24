val kotlin_version: String by project

plugins {
    kotlin("jvm") version "1.7.10" apply true
    kotlin("plugin.serialization") version "1.7.10" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        api("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

version = "3.0.0"
