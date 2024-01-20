package club.speedrun.vods.marathon

import dev.qixils.gdq.BidState
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class BidData(
    val id: Int,
    val children: List<BidData>,
    val name: String,
    val state: BidState,
    val description: String,
    val shortDescription: String,
    val goal: Float?,
    val isTarget: Boolean,
    val allowUserOptions: Boolean,
    val optionMaxLength: Int?,
    @Serializable(with = InstantAsStringSerializer::class) val revealedAt: Instant?,
    val donationTotal: Float,
    val donationCount: Int,
    val pinned: Boolean,
)