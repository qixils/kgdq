plugins {
    kotlin("jvm") version "1.8.22" apply true
    kotlin("plugin.serialization") version "1.9.0" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        api("org.jetbrains.kotlin:kotlin-stdlib:1.8.22")
    }

    tasks.compileKotlin {
        kotlinOptions.freeCompilerArgs += "-opt-in=dev.qixils.gdq.InternalGdqApi"
        kotlinOptions.freeCompilerArgs += "-opt-in=dev.qixils.horaro.InternalHoraroApi"
    }

    kotlin {
        jvmToolchain(17)
    }
}

version = "3.0.0"
