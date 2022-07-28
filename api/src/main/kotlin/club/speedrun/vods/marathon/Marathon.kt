@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.marathon

import club.speedrun.vods.db
import dev.qixils.gdq.GDQ
import dev.qixils.gdq.Hook
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.models.Bid
import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Run
import dev.qixils.gdq.models.Wrapper
import dev.qixils.horaro.Horaro
import dev.qixils.horaro.models.FullSchedule
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.litote.kmongo.setValue
import java.time.Duration
import java.time.Instant

abstract class Marathon(val api: GDQ) {
    private val eventIdCache = mutableMapOf<String, Int>()
    private val eventCacher = EventDataCacher(api)
    private val eventUpdater = EventOverrideUpdater(api)

    private suspend fun getEvent(id: String): Wrapper<Event>? {
        val eventId = id.toIntOrNull() ?: eventIdCache[id]
        if (eventId != null) {
            if (eventId == -1) return null
            return api
                .query(ModelType.EVENT, id = eventId, preLoad = eventCacher, postLoad = eventUpdater)
                .firstOrNull()
        }

        val events = ArrayList(api.query(type=ModelType.EVENT, preLoad = eventCacher, postLoad = eventUpdater))
        events.forEach { eventIdCache[it.value.short] = it.id }

        val event = events.firstOrNull { it.value.short.equals(id, true) }
        if (event != null)
            eventIdCache[id] = event.id
        return event
    }

    private suspend fun getEventId(id: String): Int? {
        val eventId = id.toIntOrNull() ?: eventIdCache[id]
        if (eventId != null) return eventId
        return getEvent(id)?.id
    }

    private suspend fun handleClassicSchedule(
        query: RunList,
        event: Wrapper<Event>
    ): List<RunData> {
        // do queries
        val runs: List<Wrapper<Run>> = ArrayList(api.query(
            type=ModelType.RUN,
            id=query.id?.toInt(),
            event=event.id,
            runner=query.runner
        )).sortedBy { it.value.order }
        val bids = ArrayList(api.query(
            type=ModelType.BID_TARGET,
            run=query.id?.toInt(),
            event=event.id
        ))

        // compute bid data
        val topLevelBidMap = bids
            .filter { it.value.parent() == null && it.value.run() != null }
            .map { it.id }
            .associateWith { mutableListOf<Bid>() }
        bids.forEach { if (it.value.parent() != null) topLevelBidMap[it.value.parent()!!.id]?.add(it.value) }

        // compute run data
        val runBidMap = runs.associate { it.id to mutableListOf<Pair<Bid, MutableList<Bid>>>() }
        topLevelBidMap.entries
            // map bid ids to bids
            .map { entry -> (bids.first { bid -> bid.id == entry.key }).value to entry.value }
            // add to runBidMap
            .forEach { runBidMap[it.first.run()!!.id]?.add(it) }

        // finalize & respond
        val runData: MutableList<RunData> = ArrayList()
        runs.forEach { run ->
            // get bids
            val rawRunBids = runBidMap[run.id]!!
            val runBids = rawRunBids.map { bid ->
                BidData(bid.first, bid.second.map { value -> BidData(value, emptyList(), run) }, run)
            }
            // get other data
            val overrides = api.db.getOrCreateRunOverrides(run)
            val previousRun = runData.lastOrNull()
            val data = RunData(run, runBids, previousRun, overrides)
            if (api is ESA) {
                // TODO: remove this god forsaken hack
                val cachedAt = api.runCachedAt[data.id] ?: Instant.EPOCH
                val now = Instant.now()
                if (Duration.between(cachedAt, now) > ModelType.RUNNER.cacheFor) {
                    api.runCachedAt[data.id] = now
                    api.query(type = ModelType.RUNNER, run = data.id)
                }
            }
            data.loadRunners()
            runData.add(data)
        }
        return runData
    }

    private suspend fun handleHoraroSchedule(
        query: RunList,
        event: Wrapper<Event>,
        schedule: FullSchedule
    ): List<RunData> {
        // this method is a bit hacky by nature of how Horaro works, but it should be stable.
        val trackerRuns = handleClassicSchedule(query, event)
        val horaroRuns = ArrayList<RunData>(schedule.items.size)
        schedule.items.forEach {  horaroRun ->
            val order = horaroRuns.size + 1
            val overrides = api.db.getOrCreateRunOverrides(horaroRun)
            val horaroId = horaroRun.getValue("ID", true)
            val trackerRun = if (horaroId == null) null
            else trackerRuns.firstOrNull { it.trackerSource?.horaroId == horaroId }
            val data = RunData(horaroRun, trackerRun, horaroRuns.lastOrNull(), event, order, overrides)
            data.loadRunners()
            horaroRuns.add(data)
        }
        return horaroRuns // TODO: filter based on query param
    }

    fun route(): Route.() -> Unit {
        return {
            get<EventList> { query ->
                // get events
                val events: List<Wrapper<Event>>
                = if (query.id != null) {
                    val event = getEvent(query.id)
                    if (event == null) emptyList() else listOf(event)
                } else {
                    ArrayList(api.query(type=ModelType.EVENT, preLoad = eventCacher, postLoad = eventUpdater))
                }
                call.respond(events.map { EventData(it) }.sortedBy { it.datetime })
            }

            get<RunList> { query ->
                // get event id and ensure it exists
                val event: Wrapper<Event>? = query.event?.let { getEvent(it) }
                if (event == null) {
                    call.respond(emptyList<RunData>())
                    return@get
                }

                // get schedule
                val schedule: FullSchedule? = event.value.horaroSchedule()

                // handle
                call.respond(
                    if (schedule != null)
                        handleHoraroSchedule(query, event, schedule)
                    else
                        handleClassicSchedule(query, event)
                )
            }
        }
    }
}

@Location("/events")
data class EventList(val id: String? = null)
@Location("/runs")
data class RunList(val id: String? = null, val event: String? = null, val runner: Int? = null, val bid: Int? = null)

class GDQMarathon : Marathon(GDQ())
class ESAMarathon : Marathon(ESA())

class ESA : GDQ("https://donations.esamarathon.com/search/", "esa") {
    val runCachedAt = mutableMapOf<Int, Instant>()
    override suspend fun cacheRunners() {
        // TODO: remove this method (and this whole subclass TBH) when ESA fixes their API
        val now = Instant.now()
        if (lastCachedRunners != null && lastCachedRunners!!.plus(ModelType.RUNNER.cacheFor).isAfter(now))
            return
        lastCachedRunners = now
        query(type = ModelType.RUNNER)
    }
}

suspend fun Event.horaroSchedule(): FullSchedule? {
    if (horaroEvent == null || horaroSchedule == null) return null
    return Horaro.getSchedule(horaroEvent!!, horaroSchedule!!)
}

class EventDataCacher(val api: GDQ) : Hook<Event> {
    override suspend fun handle(item: Wrapper<Event>) {
        if (!api.eventStartedAt.containsKey(item.id)) {
            val overrides: EventOverrides = api.db.getOrCreateEventOverrides(item.value)
            if (overrides.datetime != null)
                api.eventStartedAt[item.id] = overrides.datetime!!
        }
    }
}

class EventOverrideUpdater(val api: GDQ) : Hook<Event> {
    override suspend fun handle(item: Wrapper<Event>) = coroutineScope {
        val overrides = api.db.getOrCreateEventOverrides(item.value)
        if (overrides.datetime == null) {
            overrides.datetime = item.value.datetime
            // update db asynchronously
            launch { api.db.events.updateOneById(
                overrides._id,
                setValue(EventOverrides::datetime, overrides.datetime)
            ) }
        }
    }
}
