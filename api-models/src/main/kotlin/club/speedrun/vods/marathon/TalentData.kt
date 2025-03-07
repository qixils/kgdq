package club.speedrun.vods.marathon

import kotlinx.serialization.Serializable

@Serializable
data class TalentData(
    val name: String,
    val stream: String? = null,
    val twitter: String? = null,
    val youtube: String? = null,
    val pronouns: String? = null,
)