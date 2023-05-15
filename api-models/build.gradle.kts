plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
}

dependencies {
    api(project(":kgdq"))
    api(project(":horaro"))
    api(project(":db"))
}