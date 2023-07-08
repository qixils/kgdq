package dev.qixils.gdq.serializers

import kotlinx.serialization.ExperimentalSerializationApi
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

open class NoneFixer<DefaultOrT, T: DefaultOrT>(val delegate: KSerializer<T>, val default: DefaultOrT) : KSerializer<DefaultOrT> {
    override val descriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): DefaultOrT {
        val maybeString = decoder.runCatching { decodeString() } .getOrNull()
        return if (maybeString == "None") {
            default
        } else {
            delegate.deserialize(decoder)
        }
    }

    override fun serialize(encoder: Encoder, value: DefaultOrT) {
        if (value == default)
            encoder.encodeString("None")
        else {
            // Safety: value is of type T excluding DefaultOrT
            // all DefaultOrT values that are not T are assumed to be exactly the set of {val default}
            // assumes default implements structured equality
            val vAsDerived = value as T
            delegate.serialize(encoder, vAsDerived)
        }

    }
}

object NoneDoubleFixer : NoneFixer<Double, Double>(Double.serializer(), 0.0)
object NoneNullableDoubleFixer : NoneFixer<Double?, Double>(Double.serializer(), null)
object NoneIntFixer : NoneFixer<Int, Int>(Int.serializer(), 0)
object NoneNullableIntFixer : NoneFixer<Int, Int>(Int.serializer(), 0)