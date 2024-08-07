package club.speedrun.vods

import club.speedrun.vods.marathon.*
import club.speedrun.vods.plugins.configureHTTP
import club.speedrun.vods.plugins.configureMonitoring
import club.speedrun.vods.plugins.configureOAuth
import club.speedrun.vods.plugins.configureRouting
import club.speedrun.vods.rabbit.RabbitManager
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("SVC")
const val root = "https://vods.speedrun.club"
val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}
val gdq = GDQMarathon()
// val esa = ESAMarathon()
val hek = HEKMarathon()
val rpglb = RPGLBMarathon()
val bsg = BSGMarathon()
val marathons: List<Marathon> = listOf(gdq, /* esa, */ hek, rpglb, bsg)
val rootDb = RootDatabase()
val srcDb = SrcDatabase()
val httpClient = HttpClient(OkHttp) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json(json) }
}
val rabbit = try {
    RabbitManager()
} catch (e: Exception) {
    logger.warn("Failed to instantiate RabbitManager", e)
    null
}

fun main() {
    embeddedServer(Netty, port = 4010, host = "0.0.0.0", module = Application::kgdqApiModule).start(wait = true)
}

private val databaseManagers = mutableMapOf<String, GdqDatabase>()

fun getDB(organization: String): GdqDatabase {
    return databaseManagers.getOrPut(organization) { GdqDatabase(organization) }
}

fun Application.kgdqApiModule() {
    configureHTTP()
    configureMonitoring()
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json(json) }
    configureOAuth()
    configureRouting()
    /*
    rabbit?.declareQueue("cg_events_reddit_esa2022s1", "ESAMarathon", esa.db)
    rabbit?.declareQueue("cg_events_reddit_esa2022s2", "ESAMarathon2", esa.db)
     */
    runBlocking {
        marathons.forEach { it.api.cacheAll() }
    }
}
