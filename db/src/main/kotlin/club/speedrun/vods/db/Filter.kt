package club.speedrun.vods.db

import kotlin.reflect.KProperty1

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

        infix fun <T : Identified, V> KProperty1<T, V?>.eq(value: V?): Filter<T> = eq(this::get, value)
    }
}