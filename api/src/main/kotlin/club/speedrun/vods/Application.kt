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
import kotlinx.serialization.json.Json

const val root = "https://vods.speedrun.club"
val gdq = GDQMarathon()
val esa = ESAMarathon()
val hek = HEKMarathon()
val rpglb = RPGLBMarathon()
val srcDb = SrcDatabase()
val httpClient = HttpClient(Apache) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        json(Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        })
    }
}

fun main() {
    embeddedServer(Netty, port = 4010, host = "0.0.0.0", module = Application::kgdqApiModule).start(wait = true)
}

private val databaseManagers = mutableMapOf<String, GdqDatabase>()

fun getDB(organization: String): GdqDatabase {
    return databaseManagers.getOrPut(organization) { GdqDatabase(organization) }
}

val GDQ.db: GdqDatabase get() = getDB(organization)
fun Application.kgdqApiModule() {
    configureHTTP()
    configureMonitoring()
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json() }
    configureOAuth()
    configureRouting()
//    RabbitManager.declareQueue("cg_events_reddit_esaw2023s1", "ESAMarathon", esa.api.db)
//    RabbitManager.declareQueue("cg_events_reddit_esaw2023s2", "ESAMarathon2", esa.api.db)
}
