package club.speedrun.vods.marathon

import dev.qixils.gdq.serializers.DurationAsStringSerializer
import kotlinx.serialization.Serializable
import java.time.Duration

@Serializable
data class RunOverrides(
    val id: Int,
    var vods: MutableList<VOD> = mutableListOf(),
    @Serializable(with = DurationAsStringSerializer::class) var runTime: Duration?,
    @Serializable(with = DurationAsStringSerializer::class) var setupTime: Duration?,
)