plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":kgdq"))
    api(project(":horaro"))
    api(project(":db"))
}