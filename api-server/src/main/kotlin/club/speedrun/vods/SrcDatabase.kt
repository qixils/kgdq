package club.speedrun.vods

import club.speedrun.vods.db.Database
import club.speedrun.vods.db.Identified
import club.speedrun.vods.utils.isSkipGame
import dev.qixils.gdq.serializers.InstantAsSecondsSerializer
import dev.qixils.gdq.src.SpeedrunClient
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

// todo: migrate to cache db api

class SrcDatabase : Database("api", "src") {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val games = getCollection(SrcGame.serializer(), SrcGame.COLLECTION_NAME)
    private val cacheFor = Duration.ofDays(180)
    private val pendingCache = Channel<SrcGame>(
        capacity = Channel.Factory.UNLIMITED,
    )

    init {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(newSingleThreadContext("SrcCacheUpdater")) {
            while (true) {
                val game = pendingCache.receive()
                // Ensure the game hasn't already been cached earlier in the channel
                if (!game.abbreviation.isNullOrEmpty()) continue

                logger.info("Fetching game ${game.name}")
                game.abbreviation = SpeedrunClient.getGamesByName(game.name).firstOrNull()?.abbreviation
                game.lastChecked = Instant.now()
                games.update(game)
                delay(600.milliseconds)
            }
        }
    }

    private fun cache(game: SrcGame) {
        pendingCache.trySend(game)
    }

    fun getGame(name: String): SrcGame {
        if (isSkipGame(name)) return SrcGame(name, lastChecked = Instant.EPOCH)

        val lowerName = name.lowercase(Locale.US)
        val game = games.find { it.name.lowercase(Locale.US) == lowerName }
        if (game == null) {
            val newGame = SrcGame(name, lastChecked = Instant.EPOCH)
            games.insert(newGame)
            cache(newGame)
            return newGame
        }
        else if (game.lastChecked.isBefore(Instant.now().minus(cacheFor)))
            cache(game)
        return game
    }
}

@Serializable
data class SrcGame(
    val name: String,
    var abbreviation: String? = null, // speedrun.com abbreviation
    @Serializable(with = InstantAsSecondsSerializer::class) var lastChecked: Instant = Instant.now()
) : Identified {
    companion object {
        const val COLLECTION_NAME = "games"
    }

    override val id: String
        get() = name
}