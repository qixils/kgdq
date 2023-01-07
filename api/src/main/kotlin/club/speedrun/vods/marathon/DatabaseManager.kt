package club.speedrun.vods.marathon

import club.speedrun.vods.Identified
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

open class DatabaseManager(dbName: String) {
    val rootPath = Paths.get(System.getProperty("user.home"), "kgdq-api", dbName)

    inline fun <reified T : Identified> getCollection(name: String, serializer: KSerializer<T>): DatabaseCollection<T> {
        val path = rootPath.resolve(name)
        return DatabaseCollection(path, serializer)
    }
}

@OptIn(ExperimentalSerializationApi::class)
class DatabaseCollection<T : Identified>(private val path: Path, private val serializer: KSerializer<T>) {
    private val cache = mutableMapOf<String, T>()

    companion object {
        private val executor = Executors.newSingleThreadExecutor()
        private val sanitizer = Regex("[^a-zA-Z0-9_-]")
    }

    init {
        path.toFile().mkdirs()
        // init cache
        executor.execute {
            path.listDirectoryEntries("*.cbor").forEach {
                val obj = Cbor.decodeFromByteArray(serializer, it.readBytes())
                cache[obj.id] = obj
            }
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
        val bytes = Cbor.encodeToByteArray(serializer, obj)
        executor.execute { pathOf(id).writeBytes(bytes) }
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

fun interface Filter<T : Identified> {
    fun matches(obj: T): Boolean

    companion object {
        fun <T : Identified> and(vararg filters: Filter<T>): Filter<T> = and(filters.asIterable())
        fun <T : Identified> and(filters: Iterable<Filter<T>>): Filter<T> = Filter { filters.all { filter -> filter.matches(it) } }
        fun <T : Identified> or(vararg filters: Filter<T>): Filter<T> = or(filters.asIterable())
        fun <T : Identified> or(filters: Iterable<Filter<T>>): Filter<T> = Filter { filters.any { filter -> filter.matches(it) } }
        fun <T : Identified> not(filter: Filter<T>): Filter<T> = Filter { !filter.matches(it) }
        fun <T : Identified, V> eq(function: (T) -> V?, value: V?): Filter<T> = Filter { function(it) == value }
        fun <T : Identified> id(id: String): Filter<T> = Filter { it.id == id }
    }
}

infix fun <T : Identified, V> KProperty1<T, V?>.eq(value: V?): Filter<T> = Filter.eq(this::get, value)

fun interface Update<T : Identified> {
    fun apply(obj: T)

    companion object {
        fun <T : Identified> join(vararg updates: Update<T>): Update<T> = Update { updates.forEach { update -> update.apply(it) } }
        fun <T : Identified, V> set(function: (T, V?) -> Unit, value: V?): Update<T> = Update { function(it, value) }
    }
}

infix fun <T : Identified, V> KMutableProperty1<T, V?>.set(value: V?): Update<T> = Update.set(this::set, value)