package club.speedrun.vods.marathon

import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

@Serializable
data class EventData(
    val id: String,
    val short: String,
    val name: String,
    @Serializable(with = InstantAsStringSerializer::class)
    val startTime: Instant?,
    @Serializable(with = InstantAsStringSerializer::class)
    val endTime: Instant?,
    @Serializable(with = ZoneIdSerializer::class)
    val timezone: ZoneId?,
    val amount: Double,
    val count: Int?,
    val charityName: String?,
    val currency: String?,
    val donationUrl: String?,
    val scheduleUrl: String?,
) {
    val timeStatus: TimeStatus?

    init {
        val now = Instant.now()
        timeStatus = when {
            startTime == null -> null
            now < startTime -> TimeStatus.UPCOMING
            endTime == null -> TimeStatus.FINISHED
            now < endTime -> TimeStatus.IN_PROGRESS
            else -> TimeStatus.FINISHED
        }
    }
}