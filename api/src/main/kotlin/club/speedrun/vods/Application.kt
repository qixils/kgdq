package club.speedrun.vods

import club.speedrun.vods.plugins.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSecurity()
        configureHTTP()
        configureMonitoring()
        configureTemplating()
        install(ContentNegotiation) { json() }
        configureRouting()
    }.start(wait = true)
}
