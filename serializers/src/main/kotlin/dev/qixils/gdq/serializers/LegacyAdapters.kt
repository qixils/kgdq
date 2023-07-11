package dev.qixils.gdq.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
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

open class FnSerializer<T>(override val descriptor: SerialDescriptor, val fromJson: (JsonElement) -> T, val toJson: (JsonEncoder, T) -> Unit) : KSerializer<T> {

    override fun deserialize(decoder: Decoder): T {
        val jsonDecoder = decoder as? JsonDecoder ?: error("FnSerializer can only be used with JsonDecoder")
        return fromJson(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: T) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("FnSerializer can only be used with JsonEncoder")
        toJson(jsonEncoder, value)
    }
}

object DoubleOrNoneNullSerializer: FnSerializer<Double?>(Double.serializer().descriptor, { it.jsonPrimitive.doubleOrNull }, {
    encoder, value ->
    if (value == null)
        encoder.encodeString("None")
    else
        encoder.encodeDouble(value)
})

object DoubleOrNoneZeroSerializer: FnSerializer<Double>(Double.serializer().descriptor, { it.jsonPrimitive.doubleOrNull ?: 0.0 }, {
    encoder, value -> encoder.encodeDouble(value)
})

object IntOrNoneNullSerializer: FnSerializer<Int?>(Int.serializer().descriptor, { it.jsonPrimitive.intOrNull }, {
    encoder, value ->
    if (value == null)
        encoder.encodeString("None")
    else
        encoder.encodeInt(value)
})

object IntOrNoneZeroSerializer: FnSerializer<Int>(Int.serializer().descriptor, { it.jsonPrimitive.intOrNull ?: 0 }, {
    encoder, value -> encoder.encodeInt(value)
})
