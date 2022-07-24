@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.marathon

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.models.Bid
import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Run
import dev.qixils.gdq.models.Wrapper
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

abstract class Marathon {
    abstract val gdq: GDQ

    private suspend fun getEvent(id: String): Wrapper<Event>? {
        if (id.toIntOrNull() != null)
            return gdq.query(ModelType.EVENT, id=id.toInt()).firstOrNull()

        val events = ArrayList(gdq.query(type=ModelType.EVENT))
        if (id == "latest")
            return events.maxByOrNull { it.id }
        return events.firstOrNull { it.value.short == id }
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
                call.respond(events.sortedBy { it.value.datetime })
            }

            get<RunList> { query ->
                // do queries
                val event = query.event?.let { getEvent(it) }
                val runs: List<Wrapper<Run>> = ArrayList(gdq.query(
                    type=ModelType.RUN,
                    id=query.id,
                    event=event?.id,
                    runner=query.runner
                )).sortedBy { it.value.order }
                val bids = ArrayList(gdq.query(
                    type=ModelType.BID,
                    run=query.id,
                    event=event?.id
                ))

                // compute bid data
                val topLevelBidMap = bids
                    .filter { it.value.parent == null }
                    .associateWith { mutableListOf<Wrapper<Bid>>() }
                bids.forEach { if (it.value.parent != null) topLevelBidMap[it.value.parent]?.add(it) }

                // compute run data
                val runBidMap = runs.associate { it.id to mutableListOf<BidData>() }
                topLevelBidMap.forEach { runBidMap[it.key.value.run.id]?.add(BidData(it.key.value, it.value.map { value -> BidData(value.value, emptyList()) })) }

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
class ESAMarathon : Marathon() { override val gdq: GDQ = GDQ("https://donations.esamarathon.com/search/") }