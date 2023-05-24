plugins {
    kotlin("jvm") version "1.8.21" apply true
    kotlin("plugin.serialization") version "1.8.21" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
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
        api("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")
    }

    tasks.compileKotlin {
        kotlinOptions {
            jvmTarget = javaVer.majorVersion
        }
        kotlinOptions.freeCompilerArgs += "-opt-in=dev.qixils.gdq.InternalGdqApi"
        kotlinOptions.freeCompilerArgs += "-opt-in=dev.qixils.horaro.InternalHoraroApi"
    }
}

version = "3.0.0"
