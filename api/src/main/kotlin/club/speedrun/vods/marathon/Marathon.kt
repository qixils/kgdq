@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.marathon

import dev.qixils.gdq.GDQ
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
import java.time.Duration
import java.time.Instant

abstract class Marathon {
    abstract val gdq: GDQ

    private suspend fun getEvent(id: String): Wrapper<Event>? {
        if (id.toIntOrNull() != null)
            return gdq.query(ModelType.EVENT, id=id.toInt()).firstOrNull()

        val events = ArrayList(gdq.query(type=ModelType.EVENT))
        if (id.equals("latest", true))
            return events.maxByOrNull { it.id }
        return events.firstOrNull { it.value.short.equals(id, true) }
    }

    private suspend fun getEventId(id: String): Int? {
        if (id.toIntOrNull() != null)
            return id.toInt()
        return getEvent(id)?.id
    }

    private suspend fun handleClassicSchedule(
        query: RunList,
        event: Wrapper<Event>
    ): List<RunData> {
        // do queries
        val runs: List<Wrapper<Run>> = ArrayList(gdq.query(
            type=ModelType.RUN,
            id=query.id,
            event=event.id,
            runner=query.runner
        )).sortedBy { it.value.order }
        val bids = ArrayList(gdq.query(
            type=ModelType.BID,
            run=query.id,
            event=event.id
        ))

        // compute bid data
        val topLevelBidMap = bids
            .filter { it.value.parent() == null && it.value.run() != null }
            .associateWith { mutableListOf<Wrapper<Bid>>() }
        bids.forEach { if (it.value.parent() != null) topLevelBidMap[it.value.parent()]?.add(it) }

        // compute run data
        val runBidMap = runs.associate { it.id to mutableListOf<BidData>() }
        topLevelBidMap.forEach { runBidMap[it.key.value.run()!!.id]?.add(BidData(it.key.value, it.value.map { value -> BidData(value.value, emptyList()) })) }

        // finalize & respond
        val runData: MutableList<RunData> = ArrayList()
        runs.forEach {
            val runBids = runBidMap[it.id]!!
            val previousRun = runData.lastOrNull()
            val data = RunData(it, runBids, previousRun)
            if (gdq is ESA) {
                // TODO: remove this god forsaken hack
                val esa = gdq as ESA
                val cachedAt = esa.runCachedAt[data.id] ?: Instant.EPOCH
                val now = Instant.now()
                if (Duration.between(cachedAt, now) > ModelType.RUNNER.cacheFor) {
                    esa.runCachedAt[data.id] = now
                    gdq.query(type = ModelType.RUNNER, run = data.id)
                }
            }
            data.loadRunners(gdq)
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
            val horaroId = horaroRun.data.first { "ID".equals(it.column, true) }
            val trackerRun = trackerRuns.firstOrNull { it.trackerSource?.horaroId == horaroId.value }
            val data = RunData(horaroRun, trackerRun, horaroRuns.lastOrNull(), event, order)
            data.loadRunners(gdq)
            horaroRuns.add(data)
        }
        return horaroRuns
    }

    fun route(): Route.() -> Unit {
        return {
            get<EventList> { query ->
                val events: List<Wrapper<Event>>
                = if (query.id != null) {
                    val event = getEvent(query.id)
                    if (event == null) emptyList() else listOf(event)
                } else {
                    ArrayList(gdq.query(type=ModelType.EVENT))
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
data class RunList(val id: Int? = null, val event: String? = null, val runner: Int? = null, val bid: Int? = null)

class GDQMarathon : Marathon() { override val gdq: GDQ = GDQ() }
class ESAMarathon : Marathon() { override val gdq: GDQ = ESA() }

class ESA : GDQ("https://donations.esamarathon.com/search/") {
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
