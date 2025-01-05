package club.speedrun.vods.marathon.db

interface IDatabase {
    val events: TimedCacheManager<BaseEvent>
    val runs: TimedCacheManager<BaseRun>
    val runners: CacheManager<BaseRunner>
    val headsets: CacheManager<BaseHeadset>
    val metadata: SingletonManager<Metadata>
}