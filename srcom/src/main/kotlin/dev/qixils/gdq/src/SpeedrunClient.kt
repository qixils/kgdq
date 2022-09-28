package dev.qixils.gdq.src

import dev.qixils.gdq.src.models.BulkGame
import dev.qixils.gdq.src.models.FullGame
import dev.qixils.gdq.src.models.Model
import dev.qixils.gdq.src.models.Response
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

open class SpeedrunClient(
    private val apiKey: String? = null,
) {
    private val logger = LoggerFactory.getLogger(SpeedrunClient::class.java)
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()
    private val baseUrl = "https://www.speedrun.com/api/v1/"
    private val recentRequests = mutableListOf<Instant>()

    companion object : SpeedrunClient()

    private fun recentRequests(): List<Instant> {
        recentRequests.removeIf { it.isBefore(Instant.now().minus(1, ChronoUnit.MINUTES)) }
        return recentRequests
    }

    private suspend fun <M : Model> get(request: HttpRequest.Builder, serializer: KSerializer<M>): Response<M> {
        var delay = 1000L
        while (recentRequests().size >= 100) {
            delay(delay)
            delay *= 2
        }
        recentRequests.add(Instant.now())
        if (apiKey != null)
            request.header("X-API-Key", apiKey)
        val response = client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString()).await()
        if (response.statusCode() != 200) {
            logger.error("Request failed with status code ${response.statusCode()}: ${response.body()}")
            throw RuntimeException("Request failed with status code ${response.statusCode()}")
        }
        return json.decodeFromString(Response.serializer(serializer), response.body())
    }

    private suspend fun <M : Model> get(uri: URI, serializer: KSerializer<M>): Response<M> {
        return get(HttpRequest.newBuilder(uri), serializer)
    }

    private suspend fun <M : Model> get(path: String, serializer: KSerializer<M>): Response<M> {
        return get(URI.create("$baseUrl$path"), serializer)
    }

    suspend fun getGames(): Response<FullGame> {
        return get("games", FullGame.serializer())
    }

    suspend fun getBulkGames(): List<BulkGame> {
        val games = mutableListOf<BulkGame>()
        var response: Response<BulkGame>
        var offset = 0
        do {
            response = get("games?_bulk=yes&max=1000&offset=$offset", BulkGame.serializer())
            offset += response.pagination.size
        } while (response.pagination.size == response.pagination.max)
        return games
    }

    suspend fun getGame(id: String): FullGame? {
        return get("games/$id", FullGame.serializer()).data.firstOrNull()
    }

    suspend fun getGamesByName(name: String): List<FullGame> {
        return get("games?name=${name.encode()}", FullGame.serializer()).data
    }
}

fun String.encode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8)
}