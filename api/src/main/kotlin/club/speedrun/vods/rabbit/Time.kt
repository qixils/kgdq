package club.speedrun.vods.rabbit

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

@Serializable
data class Time(
    val iso: String,
    val unix: Double,
) {
    @Transient val instant = Instant.parse(iso)
}
