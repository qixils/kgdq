package club.speedrun.vods.marathon.gdq

import club.speedrun.vods.db.Database
import club.speedrun.vods.marathon.db.*
import java.time.Duration

class DonationTrackerDatabase(organization: String) : Database("cache", "orgs", organization), IDatabase {

    override val events = TimedCacheManager(
        getCollection(BaseEvent.serializer(), "events"),
        Duration.ofMinutes(5),
        Duration.ofDays(1),
    )

    override val runs = TimedCacheManager(
        getCollection(BaseRun.serializer(), "runs"),
        Duration.ofMinutes(5),
        Duration.ofHours(12),
    )

    override val talent = CacheManager(
        getCollection(BaseTalent.serializer(), "talent"),
        Duration.ofDays(1),
    )

    override val metadata = SingletonManager(
        getCollection(Metadata.serializer(), "metadata"),
        Metadata(),
    )
}