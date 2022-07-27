package club.speedrun.vods

import club.speedrun.vods.plugins.configureHTTP
import club.speedrun.vods.plugins.configureMonitoring
import club.speedrun.vods.plugins.configureOAuth
import club.speedrun.vods.plugins.configureRouting
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

val httpClient = HttpClient(Apache)

fun main() {
    embeddedServer(Netty, port = 4010, host = "0.0.0.0") {
        configureHTTP()
        configureMonitoring()
        install(ContentNegotiation) { json() }
        configureOAuth()
        configureRouting()
    }.start(wait = true)
}
