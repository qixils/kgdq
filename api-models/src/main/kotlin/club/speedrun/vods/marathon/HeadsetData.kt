package club.speedrun.vods.marathon

import kotlinx.serialization.Serializable

@Serializable
@Deprecated(replaceWith = ReplaceWith("Talent", "club.speedrun.vods.marathon.TalentData"), message = "merged with RunnerData", level = DeprecationLevel.ERROR)
data class HeadsetData(
    val name: String,
    val pronouns: String? = null,
)