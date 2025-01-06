plugins {
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":serializers"))
    api(project(":db"))
}