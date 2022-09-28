package dev.qixils.gdq

import dev.qixils.gdq.models.*
import kotlinx.serialization.KSerializer
import java.time.Duration
import kotlin.reflect.KClass

data class ModelType<M : Model>(
    val id: String,
    val kClass: KClass<M>,
    val serializer: KSerializer<M>,
    val cacheFor: Duration,
    val strictCache: Boolean = true,
    val aliases: List<String> = emptyList(),
) {
    val allIds = aliases + id

    companion object {
        private val types = mutableMapOf<String, ModelType<*>>()
        private fun <M : ModelType<*>> register(type: M): M {
            type.allIds.forEach { types[it] = type }
            return type
        }

        fun get(id: String): ModelType<*>? = types[id.replaceFirst("tracker.", "")]

        val BID = register(ModelType("bid", Bid::class, Bid.serializer(), Duration.ofMinutes(5)))
        val BID_TARGET = register(ModelType("bidtarget", Bid::class, Bid.serializer(), Duration.ofMinutes(5)))
        val EVENT = register(ModelType("event", Event::class, Event.serializer(), Duration.ofMinutes(5)))
        val RUN = register(ModelType("run", Run::class, Run.serializer(), Duration.ofMinutes(5), aliases = listOf("speedrun")))
        val RUNNER = register(ModelType("runner", Runner::class, Runner.serializer(), Duration.ofDays(1), strictCache = false))
        // TODO: donations, donors
    }
}