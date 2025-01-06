package club.speedrun.vods.marathon.db

import club.speedrun.vods.db.Collection
import java.time.Duration
import java.time.Instant

class TimedCacheManager<O : TimedObject>(
    collection: Collection<O>,
    /**
     * How long a cached object is valid for.
     */
    cacheLength: Duration,
    /**
     * Any objects older than the provided time before the current moment are eligible to become permanently cached.
     */
    private val cacheCutoff: Duration,
) : CacheManager<O>(collection, cacheLength) {
    override fun isValid(obj: O): Boolean {
        if (Instant.now().minus(cacheCutoff).isAfter(obj.startsAt))
            return true

        if (super.isValid(obj))
            return true

//        collection.delete(obj)
        return false
    }
}