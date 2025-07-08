package club.speedrun.vods.igdb

import club.speedrun.vods.httpClient
import club.speedrun.vods.logger
import club.speedrun.vods.utils.md5
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import okhttp3.internal.wait
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

// TODO: suspend

object IGDB {
    private val queue = mutableSetOf<String>()
    private val executor = newSingleThreadContext("IgdbCacheUpdater")
    private val clientId = System.getenv("TWITCH_CLIENT_ID") ?: throw RuntimeException("need twitch credentials")
    private val clientSecret = System.getenv("TWITCH_CLIENT_SECRET")!!
    private var _token: TwitchToken? = null
    private var sleepUntil: Instant = Instant.EPOCH
    private val ratelimit: Duration = Duration.ofMillis(250)

    private val gamesQueue = kotlinx.coroutines.channels.Channel<Pair<String, String>>(
        capacity = kotlinx.coroutines.channels.Channel.Factory.UNLIMITED,
    )

    init {
        GlobalScope.launch(executor) {
            while (true) {
                val (cacheId, gameName) = gamesQueue.receive()
                if (IGDBDatabase.games.getById(cacheId) != null) {
                    continue
                }
                println("Fetching art for game: $gameName")
                try {
                    val result = search(gameName)
                    println("Got cover: ${result.result?.cover?.imageId}")
                } catch (e: Exception) {
                    println("Failed to decode games response: $e")
                    throw e;
                }
                delay(Duration.ofMillis(250).toKotlinDuration())
            }
        }
    }


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
                append("fields artworks.image_id, artworks.artwork_type, artworks.alpha_channel, artworks.animated, cover.image_id;")
                append("search \"$gameName\";") // todo: url encode or something?
                append("where version_parent = null & game_type = (0,1,2,4,6,8,9) & themes != (42);")
                append("limit 1;")
            })
        }
//        val game = try {
//            response.body<List<IGDBGame>>()
//        } catch (e: Exception) {
//            logger.warn("Failed to decode games response", e)
//            null
//        }

        val game =
            try {
                response.body<List<IGDBGame>>()
            } catch (e: Exception) {
                logger.warn("Failed to decode games response", e)
                println(response.bodyAsText())
                throw e;
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

//        if (!queue.add(gameName)) return null

        gamesQueue.trySendBlocking(cacheId to gameName)


        return null
    }
}