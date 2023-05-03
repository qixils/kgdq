package dev.qixils.gdq.serializers

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull

object LegacyStringAdapter : JsonTransformingSerializer<List<String>>(ListSerializer(String.serializer())) {
    private val split = Regex("[,&]")

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonArray)
            return element
        if (element is JsonPrimitive)
            return JsonArray(element.contentOrNull?.split(split)?.map { JsonPrimitive(it.trim()) }?.filter { it.content.isNotEmpty() } ?: emptyList())
        throw IllegalStateException("Expected JsonArray or JsonPrimitive, got $element")
    }
}