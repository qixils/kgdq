package club.speedrun.vods.client

class MarathonClient(private val parent: SvcClient, private val organization: String) {
    suspend fun get(stats: Boolean = true) = parent.getMarathon(organization, stats)
    suspend fun getEvents() = parent.getEvents(organization)
    suspend fun getEvent(event: String) = parent.getEvent(organization, event)
    suspend fun getRuns(event: String? = null, runner: Int? = null) = parent.getRuns(organization, event, runner)
    suspend fun getRun(id: String) = parent.getRun(organization, id)
}