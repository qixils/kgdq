package club.speedrun.vods.marathon

import kotlinx.serialization.Serializable

@Serializable
data class HeadsetData(
    val name: String,
    val pronouns: String? = null,
)