package club.speedrun.vods.db

import kotlinx.serialization.KSerializer

open class Database(private vararg val dbName: String) {
    fun <T : Identified> getCollection(name: String, serializer: KSerializer<T>): Collection<T> {
        return Collection(serializer, *dbName, name)
    }
}