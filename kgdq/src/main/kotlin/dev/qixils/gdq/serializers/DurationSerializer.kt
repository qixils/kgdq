package dev.qixils.gdq.serializers

import kotlinx.serialization.KSerializer
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

        fun format(hours: Int, minutes: Int, seconds: Int): String = format.format(hours, minutes, seconds)
        fun format(duration: Duration): String = format(duration.toHours().toInt(), duration.toMinutesPart(), duration.toSecondsPart())
        fun decode(input: String): Duration {
            val matcher = pattern.matcher(input)
            if (!matcher.matches())
                throw IllegalArgumentException("Invalid duration format")
            val parts = matcher.group(0).split(":")
            return (parts[0].toInt().hours + parts[1].toInt().minutes + parts[2].toInt().seconds).toJavaDuration()
        }
    }
}
