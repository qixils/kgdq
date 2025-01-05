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

interface OrganizationConfig {
    /**
     * The identifier of this organization.
     */
    val id: String
    /**
     * The short name of this organization.
     */
    val shortName: String
    /**
     * The display name of this organization.
     */
    val displayName: String
    // TODO: move descriptions here?
    /**
     * The homepage URL of this organization.
     */
    val homepageUrl: String
    /**
     * Whether this organization supports automatic VOD link generation.
     */
    val autoVODs: Boolean
    /**
     * Creates the URL of the donation page for a given event.
     */
    fun getDonationUrl(event: EventData): String
    /**
     * Creates the URL of the schedule page for a given event.
     */
    fun getScheduleUrl(event: EventData): String
}

object EmptyOrganizationConfig : OrganizationConfig {
    override val id: String = ""
    override val shortName: String = ""
    override val displayName: String = ""
    override val homepageUrl: String = ""
    override val autoVODs: Boolean = false
    override fun getDonationUrl(event: EventData): String = ""
    override fun getScheduleUrl(event: EventData): String = ""

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
