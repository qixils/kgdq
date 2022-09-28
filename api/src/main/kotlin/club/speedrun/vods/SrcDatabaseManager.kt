package club.speedrun.vods

import club.speedrun.vods.marathon.DatabaseManager
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.src.SpeedrunClient
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class SrcDatabaseManager : DatabaseManager("kgdq-api-misc-src") {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val games = db.getCollection<SrcGame>(SrcGame.COLLECTION_NAME)
    private val cacheFor = Duration.ofDays(365)

    suspend fun getOrCreateGame(name: String): SrcGame {
        var game = games.findOne(SrcGame::gameName eq name)
        val check = game == null || game.lastChecked < Instant.now().minus(cacheFor)
        if (!check)
            return game!!

        logger.info("Fetching game $name from SRC")

        val insert = game == null
        if (game == null)
            game = SrcGame(name)

        game.srcId = SpeedrunClient.getGamesByName(name).firstOrNull()?.abbreviation
        game.lastChecked = Instant.now()

        if (insert)
            games.insertOne(game)
        else
            games.updateOne(SrcGame::gameName eq name, game) // TODO: test this

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