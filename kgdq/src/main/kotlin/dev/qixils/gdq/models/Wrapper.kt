package dev.qixils.gdq.models

import dev.qixils.gdq.ModelType
import kotlinx.serialization.Serializable

@Serializable
data class Wrapper<T : Model>(private val model: String, private val pk: Int, private val fields: T) {
    val modelType = ModelType.get(model)

    fun getId(): Int {
        return pk
    }

    fun getModel(): T {
        return fields
    }
}
