package club.speedrun.vods.db

import kotlinx.serialization.KSerializer

open class Database(private vararg val dbName: String) {
    fun <T : Identified> getCollection(serializer: KSerializer<T>, name: String): Collection<T> {
        return Collection(serializer, *dbName, name)
    }
}