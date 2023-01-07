plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
}

dependencies {
    api(project(":kgdq"))
    api(project(":horaro"))
    api(project(":db"))
}