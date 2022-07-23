package dev.qixils.gdq.models

import dev.qixils.gdq.ModelType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Wrapper<T : Model>(
    @SerialName("model") val modelName: String,
    @SerialName("pk") val id: Int,
    @SerialName("fields") val value: T
) {
    @Transient val modelType = ModelType.get(modelName)!!
}
