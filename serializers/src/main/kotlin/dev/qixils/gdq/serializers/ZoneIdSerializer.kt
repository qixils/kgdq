package dev.qixils.gdq.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneId

class ZoneIdSerializer : KSerializer<ZoneId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZoneId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ZoneId {
        return ZoneId.of(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ZoneId) {
        encoder.encodeString(value.id)
    }
}

typealias ZoneIdAsString = @Serializable(with = ZoneIdSerializer::class) ZoneId