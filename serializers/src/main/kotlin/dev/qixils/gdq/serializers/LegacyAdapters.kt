package dev.qixils.gdq.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

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

open class NoneFixer<T : Any>(delegate: KSerializer<T>, val default: JsonElement = JsonNull) : JsonTransformingSerializer<T>(delegate) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive && element.contentOrNull == "None")
            return default
        return element
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element == default)
            return JsonPrimitive("None")
        return element
    }
}

@OptIn(ExperimentalSerializationApi::class)
class NullableSerializer<T : Any>(val delegate: KSerializer<T>) : KSerializer<T?> {
    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): T? {
        return if (decoder.decodeNotNullMark()) {
            decoder.decodeSerializableValue(delegate)
        } else {
            decoder.decodeNull()
        }
    }

    override fun serialize(encoder: Encoder, value: T?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeNotNullMark()
            encoder.encodeSerializableValue(delegate, value)
        }
    }
}

object NoneDoubleFixer : NoneFixer<Double>(Double.serializer(), JsonPrimitive("0.0"))
object NoneNullableDoubleFixer : NoneFixer<Double>(Double.serializer())
object NoneIntFixer : NoneFixer<Int>(Int.serializer(), JsonPrimitive("0"))
object NoneNullableIntFixer : NoneFixer<Int>(Int.serializer())