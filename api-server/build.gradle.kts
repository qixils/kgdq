plugins {
    application
    kotlin("plugin.serialization")
}

application {
    mainClass.set("club.speedrun.vods.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") }
}

dependencies {
    implementation(project(":api-models"))
    implementation(project(":srcom"))
    implementation(project(":db"))
    implementation(libs.logback)
    implementation(libs.amqp.client)
    implementation(libs.twitch4j.helix)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    testImplementation(libs.kotlin.test.junit)
    implementation(libs.kotlinx.serialization.cbor)
    testImplementation(libs.ktor.server.tests.jvm)
}