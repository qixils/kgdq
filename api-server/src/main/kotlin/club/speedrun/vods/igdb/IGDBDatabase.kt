package club.speedrun.vods.igdb

import club.speedrun.vods.db.Database
import club.speedrun.vods.marathon.db.CacheManager
import java.time.Duration

object IGDBDatabase : Database("cache", "igdb") {
    val games = CacheManager(
        getCollection(IGDBGameSearch.serializer(), "games"),
        Duration.ofDays(21),
    )
}