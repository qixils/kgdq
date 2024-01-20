package club.speedrun.vods.marathon

import kotlinx.serialization.Serializable

@Serializable
data class RunnerData(
    val name: String,
    val stream: String? = "",
    val twitter: String? = "",
    val youtube: String? = "",
    val pronouns: String? = "",
)