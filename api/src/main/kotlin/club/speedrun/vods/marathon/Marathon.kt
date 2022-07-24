@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.marathon

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant

abstract class Marathon {
    abstract val gdq: GDQ
    var lastCached: Instant? = null

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
                val eventId: Int? = query.event?.let { getEventId(it) }
                if (eventId == null) {
                    call.respond(emptyList<RunData>())
                    return@get
                }

                // do queries
                val runs: List<Wrapper<Run>> = ArrayList(gdq.query(
                    type=ModelType.RUN,
                    id=query.id,
                    event=eventId,
                    runner=query.runner
                )).sortedBy { it.value.order }
                val bids = ArrayList(gdq.query(
                    type=ModelType.BID,
                    run=query.id,
                    event=eventId
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
                    runData.add(RunData(it, runBids, previousRun))
                }
                call.respond(runData)
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
    override suspend fun cacheRunners() {
        // TODO: remove this when ESA fixes their API
    }
}