plugins {
    kotlin("jvm") version "1.9.24" apply true
    kotlin("plugin.serialization") version "1.9.24" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        api("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
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
