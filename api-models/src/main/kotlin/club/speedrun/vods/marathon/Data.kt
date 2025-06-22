@file:OptIn(ExperimentalSerializationApi::class)

package club.speedrun.vods.marathon

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

enum class TimeStatus {
    UPCOMING,
    IN_PROGRESS,
    FINISHED,
}

@Serializable
class OrganizationData (
    val displayName: String,
    val shortName: String,
    val homepageUrl: String,
    val autoVODs: Boolean,
    @EncodeDefault(EncodeDefault.Mode.NEVER) var amountRaised: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) var donationCount: Int? = null,
)
