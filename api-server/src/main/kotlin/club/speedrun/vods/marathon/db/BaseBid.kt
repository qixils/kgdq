package club.speedrun.vods.marathon.db

import kotlinx.serialization.Serializable

@Serializable
class BaseBid(
    val id: String,
    val name: String,
    val description: String,
    val shortDescription: String = description,
    val open: Boolean,
    val goal: Double? = null,
    val donationTotal: Double = 0.0,
    val donationCount: Int = 0,
    val isTarget: Boolean,
    val allowsUserOptions: Boolean,
    val optionMaxLength: Int? = null,
    val children: List<BaseBid>,
)