package club.speedrun.vods

import club.speedrun.vods.marathon.ESAMarathon
import club.speedrun.vods.marathon.GDQMarathon
import club.speedrun.vods.marathon.GdqDatabase
import club.speedrun.vods.marathon.HEKMarathon
import club.speedrun.vods.marathon.Marathon
import club.speedrun.vods.marathon.RPGLBMarathon
import club.speedrun.vods.plugins.configureHTTP
import club.speedrun.vods.plugins.configureMonitoring
import club.speedrun.vods.plugins.configureOAuth
import club.speedrun.vods.plugins.configureRouting
import club.speedrun.vods.rabbit.RabbitManager
import dev.qixils.gdq.GDQ
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

const val root = "https://vods.speedrun.club"
val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}
val gdq = GDQMarathon()
val esa = ESAMarathon()
val hek = HEKMarathon()
val rpglb = RPGLBMarathon()
val marathons: List<Marathon> = listOf(gdq, esa, hek, rpglb)
val rootDb = RootDatabase()
val srcDb = SrcDatabase()
val httpClient = HttpClient(Apache) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json(json) }
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
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json(json) }
    configureOAuth()
    configureRouting()
    RabbitManager.declareQueue("cg_events_reddit_esa2022s1", "ESAMarathon", esa.api.db)
    RabbitManager.declareQueue("cg_events_reddit_esa2022s2", "ESAMarathon2", esa.api.db)
    runBlocking {
        marathons.forEach { it.api.cacheAll() }
    }
}
