package club.speedrun.vods

import club.speedrun.vods.marathon.DatabaseManager
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.src.SpeedrunClient
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class SrcDatabaseManager : DatabaseManager("kgdq-api-misc-src") {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val games = db.getCollection<SrcGame>(SrcGame.COLLECTION_NAME)
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
                logger.info("Fetching game ${game.gameName}")
                game.srcId = SpeedrunClient.getGamesByName(game.gameName).firstOrNull()?.abbreviation
                launch { games.updateOne(SrcGame::gameName eq game.gameName, game) }
            }
        }
    }

    private fun cache(game: SrcGame) {
        if (pendingCache.none { it.gameName == game.gameName })
            pendingCache.add(game)
    }

    suspend fun getGame(name: String): SrcGame {
        val gameName = name.lowercase()
        val game = games.findOne(SrcGame::gameName eq gameName)
        if (game == null) {
            val newGame = SrcGame(gameName)
            games.insertOne(newGame)
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
    val gameName: String,
    var srcId: String? = null, // speedrun.com abbreviation
    @Serializable(with = InstantAsStringSerializer::class) var lastChecked: Instant = Instant.now()
) {
    companion object {
        const val COLLECTION_NAME = "games"
    }
}