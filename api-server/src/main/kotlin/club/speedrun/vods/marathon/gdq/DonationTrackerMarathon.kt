package club.speedrun.vods.marathon.gdq

import club.speedrun.vods.IMarathon
import club.speedrun.vods.RedditUtils.getRedditVODs
import club.speedrun.vods.createEvent
import club.speedrun.vods.createRun
import club.speedrun.vods.marathon.*
import club.speedrun.vods.marathon.db.BaseBid
import club.speedrun.vods.marathon.db.BaseEvent
import club.speedrun.vods.marathon.db.BaseRun
import club.speedrun.vods.marathon.db.BaseTalent
import dev.qixils.gdq.BidState
import dev.qixils.gdq.v2.DonationTracker
import dev.qixils.gdq.v2.models.Bid
import dev.qixils.gdq.v2.models.Event
import dev.qixils.gdq.v2.models.Run
import dev.qixils.gdq.v2.models.Talent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class DonationTrackerMarathon(
    val api: DonationTracker,
    override val id: String,
    override val displayName: String,
    override val homepageUrl: String,
    override val shortName: String = id.uppercase(Locale.US),
) : IMarathon {
    override val autoVODs: Boolean = false

    override val cacheDb = DonationTrackerDatabase(id)
    override val overrideDb = GdqDatabase(id)
    private val logger = LoggerFactory.getLogger("Marathon")
    private val eventIdCache = mutableMapOf<String, String>()

    private val endTimeCache = mutableMapOf<String, Pair<Instant, Instant>>() // eventSlug: (expiry, endTime)

    private suspend fun getEventEndTime(eventSlug: String): Instant? {

        val cached = endTimeCache[eventSlug]

        return if (cached == null || cached.first < Instant.now()) {
            val runs = getSchedule(eventSlug)
            val endTime = runs?.lastOrNull()?.endTime ?: return null
            // TODO: null end time on old event?

            val id = getEventId(eventSlug) ?: return null
            val event = getEvent(id) ?: return null
            val isOld = // don't check endTime again
                event.startsAt + Duration.ofDays(30) < Instant.now() // one month old OR
                        || cached?.let { // was already cached,
                            it.second + Duration.ofDays(1) < Instant.now() // ended a day ago
                                    && endTime == it.second // and hasn't changed
                        } ?: false

            val expiry = if (isOld) { Instant.MAX } else {
                maxOf( // valid until near event ends, then refresh every 5 minutes
                    Instant.now() + Duration.ofMinutes(5),
                    endTime - Duration.ofHours(1)
                )
            }
            endTimeCache.put(eventSlug, Pair(expiry, endTime))

            endTime
        } else {
            cached.second
        }
    }

    override suspend fun isWorking(): Boolean {
        val events = try {
            api.getEvents()
        } catch (e: Exception) {
            logger.warn("Disabling $id as getEvents call errored", e)
            return false
        }
        events.results.forEach(this::put) // might as well save these while we're here, lol
        return true
    }

    private fun put(event: Event): BaseEvent {
        return BaseEvent(
            event.id.toString(),
            event.short,
            event.name,
            event.datetime,
            event.timezone,
            event.amount,
            event.donationCount,
            event.receiverName,
            event.receiverShort,
            event.paypalCurrency,
            cachedAt = Instant.now(),
        ).also { cacheDb.events.put(it) }
    }

    private fun put(talent: Talent): BaseTalent {
        return BaseTalent(
            talent.id.toString(),
            talent.name,
            talent.pronouns,
            talent.url,
            Instant.now(),
        ).also { cacheDb.talent.put(it) }
    }

    private fun put(run: Run, eventId: String?, bids: List<BaseBid>): BaseRun? {
        run.runners.forEach(this::put)
        run.hosts.forEach(this::put)
        run.commentators.forEach(this::put)
        if (run.startTime == null) return null
        val _eventId = eventId ?: run.event?.id ?: return null
        return BaseRun(
            run.id.toString(),
            _eventId.toString(),
            run.name,
            run.displayName,
            run.twitchName,
            run.description,
            run.category,
            run.console,
            run.runners.map { it.id.toString() },
            run.hosts.map { it.id.toString() },
            run.commentators.map { it.id.toString() },
            run.startTime!!,
            run.runTime,
            run.setupTime,
            bids,
            run.coop,
            run.releaseYear,
            run.videoLinks.map { VOD.fromUrl(it.url) },
            cachedAt = Instant.now(),
        ).also {
            cacheDb.runs.put(it)
        }
    }

    private fun putEmptyRun(event: BaseEvent): BaseRun? {
        return BaseRun(
            "543210${event.id}",
            event.id,
            game = "No runs known for this event",
            startsAt = event.startsAt,
            cachedAt = Instant.now(),
        ).also { cacheDb.runs.put(it) }
    }

    private fun convRaw(parent: Bid, children: List<BaseBid>): BaseBid {
        return BaseBid(
            parent.id.toString(),
            parent.runId?.toString(),
            parent.name,
            parent.description,
            parent.shortDescription,
            parent.state == BidState.OPENED,
            parent.goal,
            parent.total,
            parent.count,
            parent.isTarget,
            parent.allowsUserOptions,
            parent.optionMaxLength,
            children.sortedByDescending { it.donationTotal },
        )
    }

    private fun convert(parent: Bid?, children: List<Bid>): List<BaseBid> {
        val mappedChildren = children.map { convRaw(it, emptyList()) }
        if (parent == null) return mappedChildren
        return listOf(convRaw(parent, mappedChildren))
    }

    suspend fun getEvent(id: String): BaseEvent? {
        val _id = id.lowercase()
        val realId = if (_id in eventIdCache) eventIdCache[_id].toString()
        else _id.lowercase()

        var event = cacheDb.events.getById(realId)
        if (event == null) {
            val intId = realId.toIntOrNull()
            val apiEvent =
                if (intId != null) api.getEvent(intId)
                else api.getEvents().results.find { realId.equals(it.short, ignoreCase = true) }
            if (apiEvent != null) {
                event = put(apiEvent)
            }
        }

        if (event == null)
            return cacheDb.events.getByIdForce(realId)?.obj // get cached obj

        eventIdCache[_id] = event.id
        return event
    }

    suspend fun getEventId(slug: String): String? {
        if (slug.toIntOrNull() != null) return slug
        val _slug = slug.lowercase()
        if (_slug in eventIdCache) return eventIdCache[_slug]
        return getEvent(slug)?.id
    }

    override suspend fun getSchedule(eventSlug: String): List<RunData>? = coroutineScope {
        val eventId = getEventId(eventSlug) ?: return@coroutineScope null
        val eventIdInt = eventId.toIntOrNull() ?: return@coroutineScope null
        val cachedEvent = cacheDb.events.getByIdForce(eventId)?.obj ?: return@coroutineScope null
        val eventOverrides = overrideDb.getOrCreateEventOverrides(eventId)

        val cachedRuns = cacheDb.runs.getBy { it.event == eventId }
        val runs = (if (cachedRuns.isNotEmpty() && cachedRuns.all { it.isValid }) cachedRuns.map { it.obj }
        else {
            val fetch = api.getEventRuns(eventIdInt).fetchAll()
            // empty?
            if (fetch.isEmpty()) {
                if (cachedRuns.isNotEmpty()) cachedRuns.map { it.obj }
                else {
                    // todo: how does this even work lol, does this produce a cache hit for the next one or smth
                    putEmptyRun(cachedEvent)
                    emptyList()
                }
            } else {
                val ids = fetch.map { it.id.toString() }.toSet()
                // remove orphaned runs
                cachedRuns
                    .filter { !it.isValid }
                    .map { it.obj }
                    .filter { it.id !in ids }
                    .forEach(cacheDb.runs::remove)

                // fetch bids
                val bids = api.getEventBids(eventIdInt).fetchAll().filter { it.runId != null } // get bids and filter out those with unknown runs
                val idBids = bids.associateBy { it.id } // map bids by their id to fetch parent from
                val parentBids = bids.groupBy { it.parentId }.toMutableMap() // map bids by their parent

                // de-duplicate bid wars
                val standaloneBids = parentBids[null]
                if (standaloneBids != null) parentBids[null] = standaloneBids.filter { !parentBids.containsKey(it.id) }

                val dbBids = parentBids
                    .flatMap { (id, children) -> convert(idBids[id], children) } // children are sorted inside
                    .sortedBy { it.goal ?: 0.0 }
                val runBids = dbBids.groupBy { it.runId!! } // organize bids by run (null ids were filtered out earlier)

                // return
                fetch.mapNotNull { put(
                    it,
                    eventId,
                    runBids.getOrElse(it.id.toString()) { emptyList() },
                ) }
            }
        }).sortedBy { it.startsAt }

        if (runs.isEmpty())
            return@coroutineScope emptyList()

        val loadVods = runs.first().startsAt.isBefore(Instant.now())
        val vodsFinalized = runs.last().run { this.startsAt + this.runTime }.isBefore(Instant.now().minus(7, ChronoUnit.DAYS))
        val vods =
            if (loadVods && (!eventOverrides.redditMergedIn || !vodsFinalized))
                async { getRedditVODs(cachedEvent.short) }
            else null

        val runData = mutableListOf<RunData>()
        runs.forEachIndexed { index, run ->
            val overrides = overrideDb.getOrCreateRunOverrides(run.id)
            val previousRun = runData.lastOrNull()
            val data = createRun(run, previousRun, overrides, cacheDb)
            vods?.await()?.getOrNull(index)?.let {
                val existingVODTypes = data.vods.mapTo(mutableSetOf()) { vod -> vod.type }
                val newVODs = it.filter { vod -> vod.type !in existingVODTypes }
                data.vods.addAll(newVODs)
                if (vodsFinalized) {
                    overrides.vods.addAll(newVODs)
                    overrideDb.runs.update(overrides)
                }
            }
            data.vods.addAll(run.vods) // add after reddit overrides update
            data.vods.sortBy { it.type }
            runData.add(data)
        }

        // update override
        if (loadVods && !eventOverrides.redditMergedIn && vodsFinalized) {
            eventOverrides.redditMergedIn = true
            overrideDb.events.update(eventOverrides)
        }

        return@coroutineScope runData
    }

    override fun getDonationUrl(event: BaseEvent): String {
        return "https://gamesdonequick.com/tracker/ui/donate/${event.id}"
    }

    override fun getScheduleUrl(event: BaseEvent): String {
        return "https://gamesdonequick.com/schedule/${event.id}"
    }

    override val organizationData: OrganizationData
    get() {
        val events = cacheDb.events.getAll().map { it.obj }
        val donationAmount = if (events.any { it.donationAmount != null }) events.sumOf { it.donationAmount ?: 0.0 } else null
        val donationCount = if (events.any { it.donationCount != null }) events.sumOf { it.donationCount ?: 0 } else null
        return OrganizationData(displayName, shortName, homepageUrl, autoVODs, donationAmount, donationCount)
    }

    override suspend fun getEventsData(skipLoad: Boolean): List<EventData> {
        if (!skipLoad && Duration.between(cacheDb.metadata.get().eventsCachedAt, Instant.now()) < cacheDb.events.cacheLength)
            api.getEvents().results.forEach(this::put)

        return cacheDb.events.getAll().map { createEvent(this, it.obj, getEventEndTime(it.obj.id)) }
    }

    override suspend fun getEventData(eventSlug: String): EventData? {
        return getEvent(eventSlug)?.let { createEvent(this, it, getEventEndTime(it.id)) }
    }
}