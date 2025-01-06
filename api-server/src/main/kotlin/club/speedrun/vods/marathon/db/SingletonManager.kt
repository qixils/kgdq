package club.speedrun.vods.marathon.db

import club.speedrun.vods.db.Collection
import club.speedrun.vods.db.Identified

class SingletonManager<O : Identified>(
    private val collection: Collection<O>,
    private val default: O,
) {
    fun get(): O = collection.get(default.id) ?: default
    fun set(obj: O) = collection.insert(obj)
}