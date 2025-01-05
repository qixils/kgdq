package dev.qixils.gdq.v1.models

import dev.qixils.gdq.v1.ModelType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Deprecated(message = "Use v2 API")
@Serializable
data class Wrapper<T : Model>(
    @SerialName("model") val modelName: String,
    @SerialName("pk") val id: Int,
    @SerialName("fields") val value: T
) {
    constructor(modelType: ModelType<T>, id: Int, value: T) : this(modelType.id, id, value)

    @Transient val modelType = ModelType.get(modelName) ?: throw IllegalArgumentException("Unknown model type: $modelName")
}
