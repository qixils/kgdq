package dev.qixils.gdq

import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Model
import dev.qixils.gdq.models.Runner

data class ModelType<T : Model>(val id: String, val clazz: Class<T>) {
    companion object {
        private val types = mutableMapOf<String, ModelType<*>>()
        private fun <M : ModelType<*>> register(type: M): M {
            types[type.id] = type
            return type
        }

        fun get(id: String): ModelType<*>? = types[id]

        val EVENT: ModelType<Event> = register(ModelType("event", Event::class.java))
        val RUNNER: ModelType<Runner> = register(ModelType("runner", Runner::class.java))
    }
}