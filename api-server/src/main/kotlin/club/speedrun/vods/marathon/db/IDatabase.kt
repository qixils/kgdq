package club.speedrun.vods.marathon.db

interface IDatabase {
    val events: TimedCacheManager<BaseEvent>
    val runs: TimedCacheManager<BaseRun>
    val talent: CacheManager<BaseTalent>
    val metadata: SingletonManager<Metadata>
}