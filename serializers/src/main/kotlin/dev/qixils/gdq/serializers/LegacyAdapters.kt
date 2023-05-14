package dev.qixils.gdq.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull

abstract class AbstractLegacyStringAdapter<T : Any>(tSerializer: KSerializer<T>) : JsonTransformingSerializer<T>(tSerializer) {
    private val split = Regex("[,&]")

    abstract fun fromPrimitive(primitive: JsonPrimitive): JsonElement?

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonArray)
            return element
        if (element is JsonPrimitive)
            return JsonArray(element.contentOrNull?.split(split)?.mapNotNull { fromPrimitive(JsonPrimitive(it.trim())) } ?: emptyList())
        throw IllegalStateException("Expected JsonArray or JsonPrimitive, got $element")
    }
}

object LegacyStringAdapter : AbstractLegacyStringAdapter<List<String>>(ListSerializer(String.serializer())) {
    override fun fromPrimitive(primitive: JsonPrimitive): JsonElement? {
        if (primitive.content.isEmpty())
            return null
        return primitive
    }
}