package club.speedrun.vods

import club.speedrun.vods.db.Database
import club.speedrun.vods.db.Identified
import dev.qixils.gdq.serializers.InstantAsSecondsSerializer
import dev.qixils.gdq.src.SpeedrunClient
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

class SrcDatabase : Database("api", "src") {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val games = getCollection(SrcGame.serializer(), SrcGame.COLLECTION_NAME)
    private val cacheFor = Duration.ofDays(365)
    private val pendingCache = mutableListOf<SrcGame>()

    init {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(newSingleThreadContext("SrcCacheUpdater")) {
            while (true) {
                val game = pendingCache.removeFirstOrNull()
                if (game == null) {
                    delay(1000)
                    continue
                }
                logger.info("Fetching game ${game.name}")
                game.abbreviation = SpeedrunClient.getGamesByName(game.name).firstOrNull()?.abbreviation
                games.update(game)
            }
        }
    }

    private fun cache(game: SrcGame) {
        if (pendingCache.none { it.name == game.name })
            pendingCache.add(game)
    }

    fun getGame(name: String): SrcGame {
        val lowerName = name.lowercase(Locale.US)
        val game = games.find { it.name.lowercase(Locale.US) == lowerName }
        if (game == null) {
            val newGame = SrcGame(name)
            games.insert(newGame)
            cache(newGame)
            return newGame
        }
        if (game.lastChecked.isBefore(Instant.now().minus(cacheFor)))
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