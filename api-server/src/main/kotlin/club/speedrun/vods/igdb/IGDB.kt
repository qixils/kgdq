package club.speedrun.vods.igdb

import club.speedrun.vods.httpClient
import club.speedrun.vods.logger
import club.speedrun.vods.utils.isSkipGame
import club.speedrun.vods.utils.md5
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

object IGDB {
    private val executor = newSingleThreadContext("IgdbCacheUpdater")
    private val clientId = System.getenv("TWITCH_CLIENT_ID") ?: throw RuntimeException("need twitch credentials")
    private val clientSecret = System.getenv("TWITCH_CLIENT_SECRET")!!
    private var _token: TwitchToken? = null
    private val gamesQueue = Channel<Pair<String, String>>(
        capacity = Channel.Factory.UNLIMITED,
    )

    init {
        GlobalScope.launch(executor) {
            while (true) {
                val (cacheId, gameName) = gamesQueue.receive()
                // Ensure the game hasn't already been cached earlier in the channel
                if (IGDBDatabase.games.getById(cacheId) != null) {
                    continue
                }
                logger.info("Fetching art for game: $gameName")
                try {
                    val result = search(gameName)
                    logger.info("Got cover: ${result.result?.cover?.imageId}")
                } catch (e: Exception) {
                    logger.error("Failed to decode games response: $e")
//                    throw e;
                }
                // Avoid API ratelimits (4req/s)
                delay(250.milliseconds)
            }
        }
    }

    private suspend fun token(): TwitchToken {
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

    private suspend fun search(gameName: String): IGDBGameSearch {
        val cacheId = gameName.lowercase().md5()
        val cache = IGDBDatabase.games.getById(cacheId)
        if (cache != null) return cache

        val response = httpClient.post("https://api.igdb.com/v4/games") {
            header("Client-ID", clientId)
            header("Authorization", token().authorization)
            setBody(buildString {
                append("fields artworks.image_id, artworks.artwork_type, artworks.alpha_channel, artworks.animated, cover.image_id, first_release_date, name;")
                append("search \"$gameName\";") // todo: url encode or something?
                append("where version_parent = null & game_type = (0,1,2,4,6,8,9,10,12) & themes != (42) & (cover != null | artworks != null);")
                append("limit 10;")
            })
        }

        val games =
            try {
                response.body<List<IGDBGame>>()
            } catch (e: Exception) {
                logger.warn("Failed to decode games response", e)
                println(response.bodyAsText())
                throw e;

            }

        // IGDB's relevancy search sucks a little bit, let's pick the game with exactly matching title, if none then oldest release
        val game = games.find { gameName.equals(it.name, ignoreCase = true) }
            ?: games.minByOrNull { it.firstReleaseDate }

        val result = IGDBGameSearch(
            cacheId,
            gameName,
            game,
            Instant.now(),
        )
        IGDBDatabase.games.put(result)
        return result
    }

    fun getCached(game: String): IGDBGameSearch? {
        // fix old gdq runs with newlines in their names
        val gameName = game.replace(Regex("\\s", RegexOption.MULTILINE), " ")

        if (isSkipGame(gameName)) return null

        val cacheId = gameName.lowercase().md5()
        val cache = IGDBDatabase.games.getById(cacheId)
        if (cache != null) return cache

        gamesQueue.trySend(cacheId to gameName)

        return null
    }

    fun clearEmptyCached() {
        IGDBDatabase.games.getAll()
            .filter { res -> res.isValid && res.obj.result == null }
            .forEach { res -> IGDBDatabase.games.remove(res.obj) }
    }
}