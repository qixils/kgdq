package club.speedrun.vods.marathon

import kotlinx.serialization.Serializable

@Serializable
data class RunnerData(
    val name: String,
    val stream: String? = null,
    val twitter: String? = null,
    val youtube: String? = null,
    val pronouns: String? = null,
)