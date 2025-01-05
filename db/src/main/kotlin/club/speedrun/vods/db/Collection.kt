package club.speedrun.vods.db

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

@OptIn(ExperimentalSerializationApi::class)
class Collection<T : Identified>(private val serializer: KSerializer<T>, vararg dbPath: String) {
    private val path = Paths.get(System.getProperty("user.home"), ".local", "share", "kgdq", *dbPath)
    private val cache = mutableMapOf<String, T>()

    companion object {
        private val logger = LoggerFactory.getLogger(Collection::class.java)
        private val cbor = Cbor {
            ignoreUnknownKeys = true
        }
        private val executor = Executors.newSingleThreadExecutor()
        private val sanitizer = Regex("[^a-zA-Z0-9_-]")
    }

    init {
        path.toFile().mkdirs()
        path.listDirectoryEntries("*.cbor").forEach {
            val obj = cbor.decodeFromByteArray(serializer, it.readBytes())
            cache[obj.id] = obj
        }
    }

    private fun sanitize(id: String) = sanitizer.replace(id, "_")

    private fun pathOf(id: String) = path.resolve(sanitize(id) + ".cbor")

    private fun pathOf(obj: T) = pathOf(obj.id)

    fun get(id: String): T? = cache[id]

    fun getAll(): List<T> = cache.values.toList()

    fun find(filter: Filter<T>): T? = cache.values.find { filter.matches(it) }

    fun findAll(filter: Filter<T>): List<T> = cache.values.filter { filter.matches(it) }

    private fun save(id: String) {
        val obj = cache[id] ?: return
        val bytes = cbor.encodeToByteArray(serializer, obj)
        executor.execute {
            val path = pathOf(id)
            logger.debug("Saving $obj to $path (${bytes.size} bytes)")
            try {
                path.writeBytes(bytes)
            } catch (e: Exception) {
                logger.error("Failed to save $obj", e)
            }
        }
    }

    fun update(obj: T) {
        cache[obj.id] = obj
        save(obj.id)
    }

    fun update(filter: Filter<T>, update: Update<T>) {
        findAll(filter).forEach { update.apply(it); update(it) }
    }

    fun updateOne(filter: Filter<T>, update: Update<T>) {
        find(filter)?.let { update.apply(it); update(it) }
    }

    fun updateById(id: String, update: Update<T>) {
        val obj = cache[id] ?: return
        update.apply(obj)
        save(id)
    }

    fun insert(obj: T) {
        cache[obj.id] = obj
        save(obj.id)
    }

    fun delete(id: String) {
        cache.remove(id)
        executor.execute { pathOf(id).deleteIfExists() }
    }

    fun delete(obj: T) = delete(obj.id)

    fun findAndDeleteAll(filter: Filter<T>): List<T> {
        val toDelete = findAll(filter)
        toDelete.forEach { delete(it) }
        return toDelete
    }

    fun findAndDelete(filter: Filter<T>): T? {
        val toDelete = find(filter)
        toDelete?.let { delete(it) }
        return toDelete
    }

    fun findAndDelete(id: String): T? {
        val toDelete = get(id)
        toDelete?.let { delete(it) }
        return toDelete
    }
}