package club.speedrun.vods.marathon

import club.speedrun.vods.db.Database
import club.speedrun.vods.rabbit.ScheduleStatus

class GdqDatabase(organization: String) : Database("api", "orgs", organization) {
    val runs = getCollection(RunOverrides.serializer(), RunOverrides.COLLECTION_NAME)
    val events = getCollection(EventOverrides.serializer(), EventOverrides.COLLECTION_NAME)
    val statuses = getCollection(ScheduleStatus.serializer(), ScheduleStatus.COLLECTION_NAME)
    val vodSuggestions = getCollection(VodSuggestion.serializer(), "vod-suggestions")

    fun getRunOverrides(runId: String): RunOverrides? {
        return runs.get(runId)
    }

    fun getOrCreateRunOverrides(runId: String): RunOverrides {
        return getRunOverrides(runId) ?: run {
            val overrides = RunOverrides(runId)
            runs.insert(overrides)
            overrides
        }
    }

//    fun getOrCreateRunOverrides(run: HoraroRun): RunOverrides? {
//        // get
//        val horaroId = run.getValue("ID") ?: return null
//        var overrides: RunOverrides? = runs.find { it.runId == horaroId }
//        // create
//        if (overrides == null) {
//            overrides = RunOverrides(runId = horaroId)
//            runs.insert(overrides)
//        }
//        // return
//        return overrides
//    }

    fun getOrCreateEventOverrides(eventId: String): EventOverrides {
        // get
        var overrides: EventOverrides? = events.get(eventId)
        // create
        if (overrides == null) {
            overrides = EventOverrides(id = eventId)
            events.insert(overrides)
        }
        // return
        return overrides
    }

    fun getOrCreateStatus(queue: String): ScheduleStatus {
        // get
        var status = statuses.get(queue)
        // create
        if (status == null) {
            status = ScheduleStatus(queue)
            statuses.insert(status)
        }
        // return
        return status
    }
}