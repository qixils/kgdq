package dev.qixils.gdq.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Duration {
        return decode(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeString(format(value))
    }

    companion object {
        private val pattern: Pattern = Pattern.compile("\\d+:\\d{2}:\\d{2}")
        private const val format: String = "%d:%02d:%02d"

        fun format(duration: Duration): String = format.format(duration.toHours().toInt(), duration.toMinutesPart(), duration.toSecondsPart())
        fun decode(input: String): Duration {
            // fix for GDQ API returning "0" instead of "0:00:00" for 0 duration
            if (input == "0")
                return Duration.ZERO
            // usual parsing
            val matcher = pattern.matcher(input)
            if (!matcher.matches())
                throw SerializationException("Invalid duration format: $input")
            val parts = matcher.group(0).split(":")
            return (parts[0].toInt().hours + parts[1].toInt().minutes + parts[2].toInt().seconds).toJavaDuration()
        }
    }
}
