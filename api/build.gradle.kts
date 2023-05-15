val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val amqp_version: String by project
val twitch4j_version: String by project
val serialization_version: String by project

plugins {
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.21"
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
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.rabbitmq:amqp-client:$amqp_version")
    implementation("com.github.twitch4j:twitch4j-helix:$twitch4j_version")
    implementation("io.ktor:ktor-server-core-jvm:2.3.0")
    implementation("io.ktor:ktor-server-auth-jvm:2.3.0")
    implementation("io.ktor:ktor-server-locations-jvm:2.3.0")
    implementation("io.ktor:ktor-client-apache-jvm:2.3.0")
    implementation("io.ktor:ktor-server-sessions-jvm:2.3.0")
    implementation("io.ktor:ktor-server-host-common-jvm:2.3.0")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.0")
    implementation("io.ktor:ktor-server-compression-jvm:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.0")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.0")
    implementation("io.ktor:ktor-server-default-headers-jvm:2.3.0")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.0")
    implementation("io.ktor:ktor-server-conditional-headers-jvm:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.0")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.0")
    implementation("io.ktor:ktor-client-core-jvm:2.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialization_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.0")
}