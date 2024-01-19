package dev.qixils.gdq.v1

import dev.qixils.gdq.v1.models.*
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

data class ModelType<M : Model>(
    val id: String,
    val kClass: KClass<M>,
    val serializer: KSerializer<M>,
    val aliases: List<String> = emptyList(),
) {
    val allIds = aliases + id

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
        val ALL_BIDS = register(ModelType("allbids", Bid::class, Bid.serializer()))

        /**
         * A donation incentive or the top-level parent of a bid war.
         */
        val BID = register(ModelType("bid", Bid::class, Bid.serializer()))

        /**
         * An option in a bid war. May also include the top-level parent of user-submittable bid wars.
         */
        val BID_TARGET = register(ModelType("bidtarget", Bid::class, Bid.serializer()))

        /**
         * An event.
         */
        val EVENT = register(ModelType("event", Event::class, Event.serializer()))

        /**
         * A speedrun.
         */
        val RUN = register(ModelType("run", Run::class, Run.serializer(), aliases = listOf("speedrun")))

        /**
         * A speedrunner.
         */
        val RUNNER = register(ModelType("runner", Runner::class, Runner.serializer()))

        /**
         * A person wearing a headset (i.e. commentators, hosts).
         */
        val HEADSET = register(ModelType("headset", Headset::class, Headset.serializer()))

        // TODO: https://github.com/GamesDoneQuick/donation-tracker/blob/master/tracker/search_filters.py _ModelMap
    }
}