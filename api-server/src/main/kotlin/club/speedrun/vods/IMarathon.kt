package club.speedrun.vods

import club.speedrun.vods.marathon.*
import club.speedrun.vods.marathon.gdq.DonationTrackerDatabase

interface IMarathon : OrganizationConfig {
    val cacheDb: DonationTrackerDatabase // todo: abstracter
    val overrideDb: GdqDatabase
    val organizationData: OrganizationData

    suspend fun isWorking(): Boolean
    suspend fun getEventsData(skipLoad: Boolean = false): List<EventData>
    suspend fun getEventData(eventSlug: String): EventData?
    suspend fun getSchedule(eventSlug: String): List<RunData>?
}