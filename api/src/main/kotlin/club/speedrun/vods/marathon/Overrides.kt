package club.speedrun.vods.marathon

import dev.qixils.gdq.serializers.DurationSerializer
import kotlinx.serialization.Serializable
import java.time.Duration

@Serializable
data class RunOverrides(
    val id: Int,
    var vods: MutableList<VOD> = mutableListOf(),
    @Serializable(with = DurationSerializer::class) var runTime: Duration?,
    @Serializable(with = DurationSerializer::class) var setupTime: Duration?,
)