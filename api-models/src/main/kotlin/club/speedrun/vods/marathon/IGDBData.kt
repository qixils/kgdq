package club.speedrun.vods.marathon

import kotlinx.serialization.Serializable

@Serializable
data class IGDBData(
    val background: String? = null,
    val cover: String? = null,
)
