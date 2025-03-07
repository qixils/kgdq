package dev.qixils.gdq.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class DurationAsStringSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationAsString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Duration {
        return decode(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString(format(value))
    }

    companion object {
        private val pattern: Pattern = Pattern.compile("\\d+:\\d{2}:\\d{2}(?:\\.\\d{1,3})?")
        private const val gdqFormat: String = "%d:%02d:%02d"

        fun format(duration: Duration, format: String = gdqFormat): String {
            if (duration.isNegative)
                throw SerializationException("Duration cannot be negative")
            return format.format(
                duration.toHours().toInt(),
                duration.toMinutesPart(),
                duration.toSecondsPart()
            )
        }

        fun decode(input: String): Duration {
            // fix for GDQ API returning "0" instead of "0:00:00" for 0 duration
            if (input == "0")
                return Duration.ZERO
            // usual parsing
            val matcher = pattern.matcher(input)
            if (!matcher.matches())
                throw SerializationException("Invalid duration format: $input")
            val parts = matcher.group(0).split(":", ".")
            return (parts[0].toInt().hours + parts[1].toInt().minutes + parts[2].toInt().seconds + (parts.getOrElse(3) { null }?.toIntOrNull() ?: 0).milliseconds).toJavaDuration()
        }
    }
}

class DurationAsSecondsSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationAsSeconds", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Duration {
        return Duration.ofSeconds(decoder.decodeLong())
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.toSeconds())
    }
}

class DurationAsMillisSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DurationAsMillis", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Duration {
        return Duration.ofMillis(decoder.decodeLong())
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.toMillis())
    }
}

typealias DurationAsString = @Serializable(with = DurationAsStringSerializer::class) Duration
typealias DurationAsSeconds = @Serializable(with = DurationAsSecondsSerializer::class) Duration
typealias DurationAsMillis = @Serializable(with = DurationAsMillisSerializer::class) Duration
