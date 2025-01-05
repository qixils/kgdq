package club.speedrun.vods.marathon.db

import club.speedrun.vods.db.Collection
import club.speedrun.vods.db.Filter
import java.time.Duration
import java.time.Instant

open class CacheManager<O : IObject>(
    protected val collection: Collection<O>,
    /**
     * How long a cached object is valid for.
     */
    val cacheLength: Duration,
) {
    protected open fun isValid(obj: O): Boolean {
        if (Instant.now().minus(cacheLength).isAfter(obj.cachedAt))
            return true

//        collection.delete(obj)
        return false
    }

    private fun toResult(obj: O): CacheResult<O> = CacheResult(obj, isValid(obj))
    private fun filterAll(objs: List<O>): List<CacheResult<O>> = objs.map(this::toResult)

    private fun filterOne(result: CacheResult<O>?): O? = if (result != null && result.isValid) result.obj else null
    private fun filterOne(obj: O?): O? = if (obj != null && isValid(obj)) obj else null

    fun getAll(): List<CacheResult<O>> = filterAll(collection.getAll())
    fun getByIdForce(id: String): CacheResult<O>? = collection.get(id)?.let { toResult(it) }
    fun getById(id: String): O? = filterOne(getByIdForce(id))
    fun getBy(filter: Filter<O>): List<CacheResult<O>> = filterAll(collection.findAll(filter))
    fun put(obj: O) = collection.insert(obj)
    fun remove(obj: O) = collection.delete(obj)
}