package club.speedrun.vods.marathon.db

import club.speedrun.vods.db.Identified
import dev.qixils.gdq.serializers.InstantAsSeconds
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Metadata(
    override val id: String = "singleton",
    var eventsCachedAt: InstantAsSeconds = Instant.EPOCH,
) : Identified
