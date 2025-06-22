package club.speedrun.vods

import club.speedrun.vods.marathon.EventData
import club.speedrun.vods.marathon.GdqDatabase
import club.speedrun.vods.marathon.OrganizationData
import club.speedrun.vods.marathon.RunData
import club.speedrun.vods.marathon.gdq.DonationTrackerDatabase

// TODO: prolly can get merged with OrganizationConfig now

interface IMarathon : OrganizationConfig {
    val cacheDb: DonationTrackerDatabase // todo: abstracter
    val overrideDb: GdqDatabase
    val organizationData: OrganizationData

    suspend fun isWorking(): Boolean
    suspend fun getEventsData(skipLoad: Boolean = false): List<EventData>
    suspend fun getEventData(eventSlug: String): EventData?
    suspend fun getSchedule(eventSlug: String): List<RunData>?
}