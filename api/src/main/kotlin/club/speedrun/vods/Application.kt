package club.speedrun.vods

import club.speedrun.vods.marathon.DatabaseManager
import club.speedrun.vods.marathon.ESAMarathon
import club.speedrun.vods.marathon.GDQMarathon
import club.speedrun.vods.plugins.configureHTTP
import club.speedrun.vods.plugins.configureMonitoring
import club.speedrun.vods.plugins.configureOAuth
import club.speedrun.vods.plugins.configureRouting
import dev.qixils.gdq.GDQ
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

val gdq = GDQMarathon()
val esa = ESAMarathon()
val httpClient = HttpClient(Apache)

fun main() {
    embeddedServer(Netty, port = 4010, host = "0.0.0.0") {
        configureHTTP()
        configureMonitoring()
        install(ContentNegotiation) { json() }
        configureOAuth()
        configureRouting()
//        RabbitManager.declareQueue("cg_events_reddit_esaw2023s1", "ESAMarathon", esa.api.db)
//        RabbitManager.declareQueue("cg_events_reddit_esaw2023s2", "ESAMarathon2", esa.api.db)
    }.start(wait = true)
}

private val databaseManagers = mutableMapOf<String, DatabaseManager>()

fun getDB(organization: String): DatabaseManager {
    return databaseManagers.getOrPut(organization) { DatabaseManager(organization) }
}

val GDQ.db: DatabaseManager get() = getDB(organization)
