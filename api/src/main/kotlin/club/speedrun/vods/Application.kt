package club.speedrun.vods

import club.speedrun.vods.marathon.*
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
val hek = HEKMarathon()
val rpglb = RPGLBMarathon()
val srcDb = SrcDatabaseManager()
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

private val databaseManagers = mutableMapOf<String, GdqDatabaseManager>()

fun getDB(organization: String): GdqDatabaseManager {
    return databaseManagers.getOrPut(organization) { GdqDatabaseManager(organization) }
}

val GDQ.db: GdqDatabaseManager get() = getDB(organization)
