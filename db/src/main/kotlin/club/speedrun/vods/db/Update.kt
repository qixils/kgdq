package club.speedrun.vods.db

import kotlin.reflect.KMutableProperty1

fun interface Update<T : Identified> {
    fun apply(obj: T)

    companion object {
        fun <T : Identified> join(vararg updates: Update<T>): Update<T> = Update { updates.forEach { update -> update.apply(it) } }
        fun <T : Identified, V> set(function: (T, V?) -> Unit, value: V?): Update<T> = Update { function(it, value) }

        infix fun <T : Identified, V> KMutableProperty1<T, V?>.set(value: V?): Update<T> = set(this::set, value)
    }
}