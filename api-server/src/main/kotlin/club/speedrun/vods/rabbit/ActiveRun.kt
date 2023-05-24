package club.speedrun.vods.rabbit

import dev.qixils.gdq.serializers.DurationAsStringSerializer
import dev.qixils.gdq.serializers.OffsetDateTimeSerializer
import dev.qixils.gdq.serializers.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.OffsetDateTime
import java.util.*

@Serializable
data class ActiveRun(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val game: String = "",
    //val teams: ???,
    val category: String = "",
    val system: String = "",
    @Serializable(with = DurationAsStringSerializer::class) val estimate: Duration? = null,
    @Serializable(with = OffsetDateTimeSerializer::class) val scheduled: OffsetDateTime? = null,
    @Serializable(with = DurationAsStringSerializer::class) val setupTime: Duration? = null,
    //val customData: ???,
    @SerialName("externalID") val horaroId: String? = null,
    val gameTwitch: String? = null,
)
