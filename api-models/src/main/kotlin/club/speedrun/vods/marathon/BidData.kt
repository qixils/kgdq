package club.speedrun.vods.marathon

import dev.qixils.gdq.BidState
import kotlinx.serialization.Serializable

@Serializable
data class BidData(
    val id: String,
    val children: List<BidData>,
    val name: String,
    val state: BidState,
    val description: String,
    val shortDescription: String,
    val goal: Double?,
    val isTarget: Boolean,
    val allowUserOptions: Boolean,
    val optionMaxLength: Int?,
    val donationTotal: Double,
    val donationCount: Int,
)