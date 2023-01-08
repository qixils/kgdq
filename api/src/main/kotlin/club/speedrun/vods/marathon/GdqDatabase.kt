package club.speedrun.vods.marathon

import club.speedrun.vods.db.Database
import club.speedrun.vods.db.Filter.Companion.eq
import club.speedrun.vods.db.Filter.Companion.or
import club.speedrun.vods.rabbit.ScheduleStatus
import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Run
import dev.qixils.gdq.models.Wrapper

class GdqDatabase(organization: String) : Database("api", "orgs", organization) {
    val runs = getCollection(RunOverrides.serializer(), RunOverrides.COLLECTION_NAME)
    val events = getCollection(EventOverrides.serializer(), EventOverrides.COLLECTION_NAME)
    val statuses = getCollection(ScheduleStatus.serializer(), ScheduleStatus.COLLECTION_NAME)

    fun getOrCreateRunOverrides(run: Wrapper<Run>): RunOverrides {
        // get
        var overrides: RunOverrides? = runs.find(or(RunOverrides::runId eq run.id, RunOverrides::horaroId eq run.value.horaroId))
        // create
        if (overrides == null) {
            overrides = RunOverrides(run)
            runs.insert(overrides)
        }
        // update
        if (overrides.runId == null) {
            overrides.runId = run.id
            runs.update(overrides)
        }
        if (overrides.horaroId == null && run.value.horaroId != null) {
            val oldOverrides = runs.findAndDelete(RunOverrides::horaroId eq run.value.horaroId)
            if (oldOverrides != null)
                overrides.mergeIn(oldOverrides)
            else
                overrides.horaroId = run.value.horaroId
            runs.update(overrides)
        }
        // return
        return overrides
    }

    fun getOrCreateRunOverrides(run: dev.qixils.horaro.models.Run): RunOverrides? {
        // get
        val horaroId = run.getValue("ID") ?: return null
        var overrides: RunOverrides? = runs.find(RunOverrides::horaroId eq horaroId)
        // create
        if (overrides == null) {
            overrides = RunOverrides(run)
            runs.insert(overrides)
        }
        // return
        return overrides
    }

    private fun updateRunOverrideIds(overrides: RunOverrides, gdqId: Int?, horaroId: String?) {
        if (overrides.runId == null && gdqId != null) {
            overrides.runId = gdqId
            runs.update(overrides)
        } else if (overrides.horaroId == null && horaroId != null) {
            overrides.horaroId = horaroId
            runs.update(overrides)
        }
    }

    fun getRunOverrides(gdqId: Int?, horaroId: String?): RunOverrides? {
        if (gdqId == null && horaroId == null)
            throw IllegalArgumentException("At least one argument must be non-null")
        // get
        val gdqIdFilter = gdqId?.let { RunOverrides::runId eq it }
        val horariIdFilter = horaroId?.let { RunOverrides::horaroId eq it }
        val overrides: RunOverrides = runs.find(or(listOfNotNull(gdqIdFilter, horariIdFilter))) ?: return null
        // update
        updateRunOverrideIds(overrides, gdqId, horaroId)
        // return
        return overrides
    }

    fun getOrCreateRunOverrides(gdqId: Int?, horaroId: String?): RunOverrides {
        if (gdqId == null && horaroId == null)
            throw IllegalArgumentException("At least one argument must be non-null")
        // get
        val gdqIdFilter = gdqId?.let { RunOverrides::runId eq it }
        val horariIdFilter = horaroId?.let { RunOverrides::horaroId eq it }
        var overrides: RunOverrides? = runs.find(or(listOfNotNull(gdqIdFilter, horariIdFilter)))
        // create
        if (overrides == null) {
            overrides = RunOverrides(runId = gdqId, horaroId = horaroId)
            runs.insert(overrides)
        }
        // update
        updateRunOverrideIds(overrides, gdqId, horaroId)
        // return
        return overrides
    }

    fun getOrCreateEventOverrides(event: Event): EventOverrides {
        // get
        var overrides: EventOverrides? = events.get(event.short)
        // create
        if (overrides == null) {
            overrides = EventOverrides(event)
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