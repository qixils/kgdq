package club.speedrun.vods.client

import club.speedrun.vods.marathon.EventData
import club.speedrun.vods.marathon.OrganizationData
import club.speedrun.vods.marathon.RunData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SvcClient(private val baseUrl: String = "https://vods.speedrun.club/api/v2") {
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun getMarathons(stats: Boolean = true) = client.get("$baseUrl/marathons?stats=$stats").body<Map<String, OrganizationData>>()
    suspend fun getMarathon(id: String, stats: Boolean = true) = client.get("$baseUrl/marathons/$id?stats=$stats").body<OrganizationData>()
    suspend fun getAllEvents() = client.get("$baseUrl/marathons/events").body<Map<String, List<EventData>>>()
    suspend fun getEvents(organization: String) = client.get("$baseUrl/marathons/$organization/events").body<List<EventData>>()
    suspend fun getEvent(organization: String, event: String) = client.get("$baseUrl/marathons/$organization/events/$event").body<EventData>()
    suspend fun getRuns(organization: String, event: String, runner: Int? = null) = client.get("$baseUrl/marathons/$organization/events/$event/runs") {
        if (runner != null) parameter("runner", runner)
    }.body<List<RunData>>()
    suspend fun getRun(organization: String, id: String) = client.get("$baseUrl/marathons/$organization/runs/$id").body<List<RunData>>().firstOrNull()

    fun getMarathonClient(id: String) = MarathonClient(this, id)
}