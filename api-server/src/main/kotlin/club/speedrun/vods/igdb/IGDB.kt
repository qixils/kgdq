package club.speedrun.vods.igdb

import club.speedrun.vods.httpClient
import club.speedrun.vods.logger
import club.speedrun.vods.utils.md5
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

// TODO: suspend

object IGDB {
    private val queue = mutableSetOf<String>()
    private val executor = newSingleThreadContext("IgdbCacheUpdater")
    private val clientId = System.getenv("TWITCH_CLIENT_ID")!!
    private val clientSecret = System.getenv("TWITCH_CLIENT_SECRET")!!
    private var _token: TwitchToken? = null
    private var sleepUntil: Instant = Instant.EPOCH
    private val ratelimit: Duration = Duration.ofMillis(250)

    suspend fun ratelimit() {
        val duration = Duration.between(Instant.now(), sleepUntil)
        sleepUntil = maxOf(Instant.now(), sleepUntil) + ratelimit
        if (duration.isPositive) {
            delay(duration.toKotlinDuration())
        }
    }

    suspend fun token(): TwitchToken {
        if (_token != null && !_token!!.isExpired) return _token!!
        val url = url {
            takeFrom("https://id.twitch.tv/oauth2/token")
            with(parameters) {
                set("client_id", clientId)
                set("client_secret", clientSecret)
                set("grant_type", "client_credentials")
            }
        }
        val response = httpClient.post(url)
        _token = response.body<TwitchToken>()
        return _token!!
    }

    suspend fun search(gameName: String): IGDBGameSearch {
        val cacheId = gameName.lowercase().md5()
        val cache = IGDBDatabase.games.getById(cacheId)
        if (cache != null) return cache

        ratelimit()
        val response = httpClient.post("https://api.igdb.com/v4/games") {
            header("Client-ID", clientId)
            header("Authorization", token().authorization)
            setBody(buildString {
                append("fields artworks.image_id, artworks.artwork_type, cover.image_id;")
                append("search \"$gameName\";") // todo: url encode or something?
                append("where version_parent = null & game_type = 0 & themes != (42);")
                append("limit 1;")
            })
        }
        val game = try {
            response.body<List<IGDBGame>>()
        } catch (e: Exception) {
            logger.warn("Failed to decode games response", e)
            null
        }

        val result = IGDBGameSearch(
            cacheId,
            gameName,
            game?.firstOrNull(),
            Instant.now(),
        )
        IGDBDatabase.games.put(result)
        return result
    }

    fun getCached(gameName: String): IGDBGameSearch? {
        val cacheId = gameName.lowercase().md5()
        val cache = IGDBDatabase.games.getById(cacheId)
        if (cache != null) return cache

        if (!queue.add(cacheId)) return null

        GlobalScope.launch(executor) {
            search(gameName)
            queue.remove(cacheId)
        }

        return null
    }
}