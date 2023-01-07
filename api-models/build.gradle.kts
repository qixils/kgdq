plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":kgdq"))
    api(project(":horaro"))
}