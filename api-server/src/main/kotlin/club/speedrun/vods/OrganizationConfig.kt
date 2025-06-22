package club.speedrun.vods

import club.speedrun.vods.marathon.db.BaseEvent

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
    fun getDonationUrl(event: BaseEvent): String
    /**
     * Creates the URL of the schedule page for a given event.
     */
    fun getScheduleUrl(event: BaseEvent): String
}