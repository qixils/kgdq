package dev.qixils.gdq

import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Model
import dev.qixils.gdq.models.Runner
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

data class ModelType<M : Model>(val id: String, val kClass: KClass<M>, val serializer: KSerializer<M>) {
    val jClass: Class<M> = kClass.java

    companion object {
        private val types = mutableMapOf<String, ModelType<*>>()
        private fun <M : ModelType<*>> register(type: M): M {
            types[type.id] = type
            return type
        }

        fun get(id: String): ModelType<*>? = types[id.replaceFirst("tracker.", "")]

        val EVENT: ModelType<Event> = register(ModelType("event", Event::class, Event.serializer()))
        val RUNNER: ModelType<Runner> = register(ModelType("runner", Runner::class, Runner.serializer()))
    }
}