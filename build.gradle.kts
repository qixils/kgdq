val kotlin_version: String by project

// TODO: move dependency versions to gradle/libs.versions.toml for dependabot

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

    val javaVer = JavaVersion.VERSION_17
    java.sourceCompatibility = javaVer
    java.targetCompatibility = javaVer

    dependencies {
        api("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVer.majorVersion
        }
        kotlinOptions.freeCompilerArgs += "-opt-in=dev.qixils.gdq.InternalGdqApi"
        kotlinOptions.freeCompilerArgs += "-opt-in=dev.qixils.horaro.InternalHoraroApi"
    }
}

version = "3.0.0"
