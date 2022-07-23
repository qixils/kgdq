package dev.qixils.gdq

import dev.qixils.gdq.models.*
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

data class ModelType<M : Model>(
    val id: String,
    val kClass: KClass<M>,
    val serializer: KSerializer<M>)
{
    val jClass: Class<M> = kClass.java

    companion object {
        private val types = mutableMapOf<String, ModelType<*>>()
        private fun <M : ModelType<*>> register(type: M): M {
            types[type.id] = type
            return type
        }

        fun get(id: String): ModelType<*>? = types[id.replaceFirst("tracker.", "")]

        val BID = register(ModelType("bid", Bid::class, Bid.serializer()))
        val BID_TARGET = register(ModelType("bidtarget", Bid::class, Bid.serializer()))
        val EVENT = register(ModelType("event", Event::class, Event.serializer()))
        val RUN = register(ModelType("run", Run::class, Run.serializer()))
        val RUNNER = register(ModelType("runner", Runner::class, Runner.serializer()))
    }
}