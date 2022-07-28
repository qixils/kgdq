package club.speedrun.vods.marathon

import club.speedrun.vods.rabbit.ScheduleStatus
import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Run
import dev.qixils.gdq.models.Wrapper
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.coroutine.replaceOne
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.setValue

class DatabaseManager(organization: String) {
    companion object {
        private val dbClient = KMongo.createClient("mongodb+srv://" +
                System.getenv("MONGO_USERNAME") +
                ":${System.getenv("MONGO_PASSWORD")}" +
                "@${System.getenv("MONGO_URL")}" +
                "/?retryWrites=true&w=majority").coroutine
    }

    private val db = dbClient.getDatabase("kgdq-api-$organization")
    val runs = db.getCollection<RunOverrides>(RunOverrides.COLLECTION_NAME)
    val events = db.getCollection<EventOverrides>(EventOverrides.COLLECTION_NAME)
    val statuses = db.getCollection<ScheduleStatus>(ScheduleStatus.COLLECTION_NAME)

    suspend fun getOrCreateRunOverrides(run: Wrapper<Run>): RunOverrides {
        // get
        var overrides: RunOverrides? = runs.findOne(RunOverrides::runId eq run.id)
            ?: runs.findOne(RunOverrides::horaroId eq run.value.horaroId)
        // create
        if (overrides == null) {
            overrides = RunOverrides(run)
            runs.insertOne(overrides)
        }
        // update
        if (overrides.runId == null) {
            overrides.runId = run.id
            runs.updateOneById(overrides._id, setValue(RunOverrides::runId, run.id))
        }
        if (overrides.horaroId == null && run.value.horaroId != null) {
            val oldOverrides = runs.findOneAndDelete(RunOverrides::horaroId eq run.value.horaroId)
            if (oldOverrides != null) {
                overrides.mergeIn(oldOverrides)
                runs.replaceOne(overrides)
            } else {
                overrides.horaroId = run.value.horaroId
                runs.updateOneById(
                    overrides._id,
                    setValue(RunOverrides::horaroId, run.value.horaroId)
                )
            }
        }
        // return
        return overrides
    }

    suspend fun getOrCreateRunOverrides(run: dev.qixils.horaro.models.Run): RunOverrides? {
        // get
        val horaroId = run.getValue("ID", true) ?: return null
        var overrides: RunOverrides? = runs.findOne(RunOverrides::horaroId eq horaroId)
        // create
        if (overrides == null) {
            overrides = RunOverrides(run)
            runs.insertOne(overrides)
        }
        // return
        return overrides
    }

    suspend fun getOrCreateEventOverrides(event: Event): EventOverrides {
        // get
        var overrides: EventOverrides? = events.findOne(EventOverrides::_id eq event.short)
        // create
        if (overrides == null) {
            overrides = EventOverrides(event)
            events.insertOne(overrides)
        }
        // return
        return overrides
    }

    suspend fun getOrCreateStatus(queue: String): ScheduleStatus {
        // get
        var status = statuses.findOneById(queue)
        // create
        if (status == null) {
            status = ScheduleStatus(queue)
            statuses.insertOne(status)
        }
        // return
        return status
    }
}