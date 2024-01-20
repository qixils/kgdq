package club.speedrun.vods.marathon

import dev.qixils.gdq.serializers.DurationAsStringSerializer
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration
import java.time.Instant

@Serializable
data class RunData(
    // TODO: deprecate these IDs; export RunOverrides ID for maps/sets/etc
    val gdqId: Int? = null,
    val horaroId: String? = null,

    val name: String,
    val displayName: String = name,
    val twitchName: String = "",
    val console: String = "",
    val releaseYear: Int? = null,
    val description: String = "",
    @Serializable(with = InstantAsStringSerializer::class)
    val startTime: Instant,
    @Serializable(with = InstantAsStringSerializer::class)
    val endTime: Instant,
    @Serializable(with = DurationAsStringSerializer::class)
    val runTime: Duration,
    @Serializable(with = DurationAsStringSerializer::class)
    val setupTime: Duration = Duration.ZERO,
    val coop: Boolean = false,
    val category: String = "",
    val runners: List<RunnerData> = listOf(),
    val commentators: List<HeadsetData> = listOf(),
    val hosts: List<HeadsetData> = listOf(),
    val bids: List<BidData> = listOf(),
    val vods: List<VOD> = listOf(),
    val src: String? = null,
) {
    /**
     * The current status of the run in the schedule.
     */
    val timeStatus: TimeStatus = run {
        val now = Instant.now()
        when {
            now < startTime.minus(setupTime) -> TimeStatus.UPCOMING
            now < endTime -> TimeStatus.IN_PROGRESS
            else -> TimeStatus.FINISHED
        }
    }

    /**
     * Whether this is the current run being played at the event.
     */
    @Transient
    val isCurrent: Boolean = timeStatus == TimeStatus.IN_PROGRESS
}