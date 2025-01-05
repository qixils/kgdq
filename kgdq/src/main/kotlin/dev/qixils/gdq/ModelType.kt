package dev.qixils.gdq

import dev.qixils.gdq.ModelType.Companion.BID
import dev.qixils.gdq.ModelType.Companion.BID_TARGET
import dev.qixils.gdq.models.*
import kotlinx.serialization.KSerializer
import java.time.Duration
import kotlin.reflect.KClass

data class ModelType<M : Model>(
    val id: String,
    val kClass: KClass<M>,
    val serializer: KSerializer<M>,
    val cacheType: CacheType,
    val aliases: List<String> = emptyList(),
) {
    val allIds = aliases + id
    val cacheFor: Duration get() = cacheType.duration
    val strictCache: Boolean get() = cacheType.strict

    companion object {
        private val types = mutableMapOf<String, ModelType<*>>()
        private fun <M : ModelType<*>> register(type: M): M {
            type.allIds.forEach { types[it] = type }
            return type
        }

        /**
         * Gets the model type for the given ID.
         *
         * @param id The ID of the model type.
         * @return The model type, or `null` if not found.
         */
        fun get(id: String): ModelType<*>? = types[id.replaceFirst("tracker.", "")]

        /**
         * The collection of all known model types.
         */
        val ALL: Set<ModelType<*>> get() = types.values.toSet()

        /**
         * Any type of bid.
         *
         * @see BID
         * @see BID_TARGET
         */
        val ALL_BIDS = register(ModelType("allbids", Bid::class, Bid.serializer(), CacheType.BID, aliases = listOf("tracker.bid")))

        /**
         * A donation incentive or the top-level parent of a bid war.
         */
        val BID = register(ModelType("bid", Bid::class, Bid.serializer(), CacheType.BID, aliases = listOf("tracker.bid")))

        /**
         * An option in a bid war. May also include the top-level parent of user-submittable bid wars.
         */
        val BID_TARGET = register(ModelType("bidtarget", Bid::class, Bid.serializer(), CacheType.BID, aliases = listOf("tracker.bid")))

        /**
         * An event.
         */
        val EVENT = register(ModelType("event", Event::class, Event.serializer(), CacheType.EVENT, aliases = listOf("tracker.event")))

        /**
         * A speedrun.
         */
        val RUN = register(ModelType("run", Run::class, Run.serializer(), CacheType.BID, aliases = listOf("speedrun", "tracker.speedrun")))

        /**
         * A speedrunner.
         */
        val RUNNER = register(ModelType("runner", Runner::class, Runner.serializer(), CacheType.RUNNER, aliases = listOf("tracker.talent")))

        /**
         * A person wearing a headset (i.e. commentators, hosts).
         */
        val HEADSET = register(ModelType("headset", Headset::class, Headset.serializer(), CacheType.HEADSET, aliases = listOf("tracker.talent")))

        // TODO: https://github.com/GamesDoneQuick/donation-tracker/blob/master/tracker/search_filters.py _ModelMap
    }
}