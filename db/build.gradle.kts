plugins {
    kotlin("plugin.serialization")
}

dependencies {
    implementation(libs.slf4j)
    implementation(libs.kotlinx.serialization.cbor)
}