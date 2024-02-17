@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.marathon

import club.speedrun.vods.*
import club.speedrun.vods.plugins.UserError
import dev.qixils.gdq.*
import dev.qixils.gdq.v1.*
import dev.qixils.gdq.v1.models.Bid
import dev.qixils.gdq.v1.models.Event
import dev.qixils.gdq.v1.models.Run
import dev.qixils.gdq.v1.models.Wrapper
import dev.qixils.horaro.Horaro
import dev.qixils.horaro.models.FullSchedule
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Predicate
import java.util.regex.Pattern
import kotlin.collections.set

abstract class Marathon(
    /**
     * The instance of the marathon's donation tracker API.
     */
    val api: GDQ,

    /**
     * The identifier of this organization.
     */
    override val id: String,
    /**
     * The display name of this organization.
     */
    override val displayName: String,
    /**
     * The homepage URL of this organization.
     */
    override val homepageUrl: String,
    /**
     * The short name of this organization.
     */
    override val shortName: String = id.uppercase(Locale.US),
    /**
     * Whether this organization supports automatic VOD link generation.
     */
    override val autoVODs: Boolean = false,
) : OrganizationConfig {
    private val logger = LoggerFactory.getLogger("Marathon")
    val eventIdCache = mutableMapOf<String, Int>() // TODO: these should always be stored as lowercase
    private val eventCacher = EventDataCacher(this, false)
    private val eventLoadSkipper = EventDataCacher(this, true)
    private val eventUpdater = EventOverrideUpdater(this)
    val db: GdqDatabase get() = getDB(id)

    private suspend inline fun <reified T> getRedditWiki(id: String, logErrors: Boolean = true): List<T> {
        val response = httpClient.get("https://www.reddit.com/r/VODThread/wiki/$id.json") {
            header(HttpHeaders.UserAgent, "GDQ VODs API v2 (/u/noellekiq)")
        }
        if (response.status != HttpStatusCode.OK) {
            if (logErrors) logger.warn("Failed to fetch wiki for $id: ${response.status}")
            return emptyList()
        }
        // strip leading spaces and trailing comments
        val body: JsonObject = response.body()
        val content = body["data"]?.jsonObject?.get("content_md")?.jsonPrimitive?.content
            ?.split(Pattern.compile("\\r?\\n"))
            ?.joinToString("") { it.split('#', limit=2)[0].trim() }
            ?: return emptyList()
        return json.decodeFromString(content)
    }

    private suspend inline fun getRedditTwitchVODs(short: String, logErrors: Boolean = true): List<List<VOD>> {
        return getRedditWiki<List<String>>("${short}vods", logErrors).map {
            val strs = it.toMutableList()
            val vods = mutableListOf<VOD>()
            while (strs.size >= 2) {
                val videoId = strs.removeFirst()
                val timestamp = strs.removeFirst()
                try {
                    vods.add(VODType.TWITCH.fromParts(videoId, timestamp))
                } catch (e: Exception) {
                    if (logErrors) logger.warn("Failed to parse Twitch VOD $videoId ($timestamp) for $short", e)
                }
            }
            if (strs.isNotEmpty() && logErrors)
                logger.warn("Excess data found for Twitch VODs for $short: ${strs.first()}")
            vods
        }
    }

    private suspend inline fun getRedditYouTubeVODs(short: String): List<List<VOD>> {
        return getRedditWiki<JsonElement>("${short}yt").map { element ->
            val strs = if (element is JsonArray)
                element.map { it.jsonPrimitive.content }
            else
                listOf(element.jsonPrimitive.content)
            strs.mapNotNull {
                try {
                    VODType.YOUTUBE.fromUrl("https://youtu.be/$it")
                } catch (e: Exception) {
                    logger.warn("Failed to parse YouTube VOD $it for $short", e)
                    null
                }
            }
        }
    }

    private suspend inline fun getRedditVODs(short: String): List<List<VOD>> {
        val twitch = try { getRedditTwitchVODs(short) } catch (e: Exception) { logger.error("Failed to load Twitch VODs for $short", e); emptyList() }
        val yt = try { getRedditYouTubeVODs(short) } catch (e: Exception) { logger.error("Failed to load YouTube VODs for $short", e); emptyList() }
        // merge
        val vods = mutableListOf<List<VOD>>()
        for (i in 0 until maxOf(twitch.size, yt.size)) {
            val list = mutableListOf<VOD>()
            if (i < twitch.size) list.addAll(twitch[i])
            if (i < yt.size) list.addAll(yt[i])
            vods.add(list)
        }
        return vods
    }

    private suspend fun getEvent(id: String, skipLoad: Boolean = false): Event? {
        val cacher = if (skipLoad) eventLoadSkipper else eventCacher
        val updater = if (skipLoad) null else eventUpdater

        val eventId = id.toIntOrNull() ?: eventIdCache[id]
        if (eventId != null) {
            if (eventId == -1) return null
            return api.getEvent(eventId, preLoad = cacher, postLoad = updater)
        }

        val events = ArrayList(api.getEvents(preLoad = cacher, postLoad = updater))
        events.forEach { eventIdCache[it.short] = it.id }

        val event = events.firstOrNull { it.short.equals(id, true) }
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
        event: Wrapper<Event>,
        eventOverrides: EventOverrides,
    ): List<RunData> = coroutineScope {
        val vodsFinalized = event.value.endTime == null || event.value.endTime!!.isBefore(Instant.now().minus(7, ChronoUnit.DAYS))
        val vods: Deferred<List<List<VOD>>>? =
            if (!eventOverrides.redditMergedIn || !vodsFinalized)
                async { getRedditVODs(event.value.short) }
            else
                null

        // do queries
        val runs: List<Run> = ArrayList(
            api.query(
                type = ModelType.RUN,
                id = query.id?.toInt(),
                event = event.id,
                runner = query.runner
            )
        ).sortedBy { it.order }

        // TODO: pagination (if not ESA...)
        val bids = api.getBids(
            run = query.id?.toInt(),
            event = event.id
        ).distinctBy { it.id }

        // compute bid data
        val topLevelBidMap = bids
            .filter { it.fetchParent() == null && it.fetchRun() != null }
            .map { it.id }
            .associateWith { mutableListOf<Bid>() }
        bids.forEach { if (it.fetchParent() != null) topLevelBidMap[it.fetchParent()!!.id]?.add(it) }

        // compute run data
        val runBidMap = runs.associate { it.id to mutableListOf<Pair<Bid, MutableList<Bid>>>() }
        topLevelBidMap.entries
            // map bid ids to bids
            .map { entry -> (bids.first { bid -> bid.id == entry.key }) to entry.value }
            // add to runBidMap
            .forEach { runBidMap[it.first.fetchRun()!!.id]?.add(it) }

        // finalize & respond
        val runData: MutableList<RunData> = ArrayList()
        runs.forEachIndexed { index, run ->
            // get bids
            val rawRunBids = runBidMap[run.id]!!
            val runBids = rawRunBids.map { bid ->
                val children = bid.second
                    .map { value -> createBid(value, emptyList(), run) }
                    .sortedByDescending { it.donationTotal }
                createBid(bid.first, children, run)
            }.sortedWith(compareBy<BidData>{ it.revealedAt }.thenBy{ it.id })
            // get other data
            val overrides = db.getOrCreateRunOverrides(run)
            val previousRun = runData.lastOrNull()
            // create run data
            val data = createRun(run, runBids, previousRun, overrides)
            data.loadSrcGame(overrides)
            vods?.await()?.getOrNull(index)?.let {
                val existingVODTypes: Set<VODType> = data.vods.mapTo(mutableSetOf()) { vod -> vod.type }
                val newVODs = it.filter { vod -> vod.type !in existingVODTypes }
                data.vods.addAll(newVODs)
                if (vodsFinalized) {
                    overrides.vods.addAll(newVODs)
                    db.runs.update(overrides)
                }
            }
            runData.add(data)
            if (data.setupTime != run.value.setupTime) {
                logger.atDebug().log { "Setup time mismatch for run ${run.value.name} (${run.id}): ${data.setupTime} != ${run.value.setupTime}" }
            }
            if (data.startTime != run.value.startTime) {
                logger.atDebug().log { "Start time mismatch for run ${run.value.name} (${run.id}): ${data.startTime} != ${run.value.startTime}" }
            }
            if (data.endTime != run.value.endTime) {
                logger.atDebug().log { "End time mismatch for run ${run.value.name} (${run.id}): ${data.endTime} != ${run.value.endTime}" }
            }
        }

        // update override
        if (!eventOverrides.redditMergedIn && vodsFinalized) {
            eventOverrides.redditMergedIn = true
            db.events.update(eventOverrides)
        }

        // return
        return@coroutineScope runData
    }

    private suspend fun handleHoraroSchedule(
        query: RunList,
        event: Wrapper<Event>,
        eventOverrides: EventOverrides,
        schedule: FullSchedule
    ): List<RunData> {
        // this method is a bit hacky by nature of how Horaro works, but it should be stable.
        val trackerRuns = handleClassicSchedule(query, event, eventOverrides)
        val horaroRuns = ArrayList<RunData>(schedule.items.size)
        schedule.items.forEach { horaroRun ->
            val order = horaroRuns.size + 1
            val overrides = db.getOrCreateRunOverrides(horaroRun)
            val horaroId = horaroRun.getValue("ID")
            val trackerRun = if (horaroId == null) null
            else trackerRuns.firstOrNull { it.trackerSource?.horaroId == horaroId }
            // create run data
            val data = RunData(horaroRun, trackerRun, horaroRuns.lastOrNull(), event, order, overrides)
            data.loadData()
            data.loadSrcGame(overrides)
            horaroRuns.add(data)
        }
        return horaroRuns.filter {
            query.id == null || (it.gdqId ?: -1) == query.id.toIntOrNull() || it.horaroId == query.id
                    //|| query.runner == null || it.runners.any { it.id == query.runner } TODO create RunnerData with added id field
                    || query.event == null || it.event == getEventId(query.event)
        }
    }

    suspend fun getEvents(query: EventList? = null, skipLoad: Boolean = false): List<Wrapper<Event>> {
        return if (query?.id != null) {
            val event = getEvent(query.id, skipLoad)
            if (event == null) emptyList() else listOf(event)
        } else {
            val cacher = if (skipLoad) eventLoadSkipper else eventCacher
            val updater = if (skipLoad) null else eventUpdater
            ArrayList(api.getEvents(preLoad = cacher, postLoad = updater))
        }
    }

    suspend fun getEventsData(query: EventList? = null, skipLoad: Boolean = false): List<EventData> {
        return getEvents(query, skipLoad).map { EventData(this, it) }.sortedBy { it.startTime }
    }

    suspend fun getOrganizationData(stats: Boolean = true): OrganizationData {
        val events = if (stats) getEventsData(skipLoad = false) else null
        return OrganizationData(this, events)
    }

    suspend fun getOrganizationData(query: Organization): OrganizationData {
        return getOrganizationData(query.stats)
    }

    fun getVodSuggestionAndRun(predicate: Predicate<VodSuggestion>): Pair<VodSuggestion, RunOverrides>? {
        for (run in db.runs.getAll()) {
            for (suggestion in run.vodSuggestions) {
                if (predicate.test(suggestion)) {
                    return Pair(suggestion, run)
                }
            }
        }
        return null
    }

    fun route(): Route.() -> Unit = {
        get<Organization> { query ->
            call.respond(getOrganizationData(query))
        }

        get<EventList> { query ->
            call.respond(getEventsData(query, query.skipLoad))
        }

        get<RunList> { query ->
            if (query.id == null && query.event == null && query.runner == null)
                throw UserError("A search parameter (id, event, or runner) is required.")

            // get event id and ensure it exists
            val event: Wrapper<Event> = query.event?.let { getEvent(it) }
                ?: throw UserError("Event ${query.event} does not exist.")
            val eventOverrides = db.getOrCreateEventOverrides(event)

            // get schedule
            val schedule: FullSchedule? = event.value.horaroSchedule()

            // handle
            call.respond(
                if (schedule != null)
                    handleHoraroSchedule(query, event, eventOverrides, schedule)
                else
                    handleClassicSchedule(query, event, eventOverrides)
            )
        }
    }
}

@Location("")
data class Organization(val stats: Boolean = true)

@Location("/events")
data class EventList(val id: String? = null, val skipLoad: Boolean = false) // TODO: move to subroute

@Location("/runs")
data class RunList(val id: String? = null, val event: String? = null, val runner: Int? = null)

class GDQMarathon : Marathon(GDQ(), "gdq", "Games Done Quick", "https://gamesdonequick.com/") {
    override fun getDonationUrl(event: EventData): String = "https://gamesdonequick.com/tracker/ui/donate/${event.short}"
    override fun getScheduleUrl(event: EventData): String = "https://gamesdonequick.com/schedule/${event.id}"
}
class ESAMarathon : Marathon(ESA(), "esa", "European Speedrunner Assembly", "https://esamarathon.com/", autoVODs = true) {
    override fun getDonationUrl(event: EventData): String = "https://donations.esamarathon.com/donate/${event.short}"
    override fun getScheduleUrl(event: EventData): String = event.horaroUrl
}
class HEKMarathon : Marathon(HEK(), "hek", "Hekathon", "https://hekathon.com/", shortName = "Hekathon") {
    override fun getDonationUrl(event: EventData): String = "https://hekathon.esamarathon.com/donate/${event.short}"
    override fun getScheduleUrl(event: EventData): String = event.horaroUrl
}
class RPGLBMarathon : Marathon(RPGLB(), "rpglb", "RPG Limit Break", "https://rpglimitbreak.com/") {
    override fun getDonationUrl(event: EventData): String = "https://rpglimitbreak.com/tracker/ui/donate/${event.short}"
    override fun getScheduleUrl(event: EventData): String = "https://rpglimitbreak.com/tracker/runs/${event.id}"
}
class BSGMarathon : Marathon(BSG(), "bsg", "Benelux Speedrunner Gathering", "https://bsgmarathon.com/") {
    override fun getDonationUrl(event: EventData): String = "https://tracker.bsgmarathon.com/ui/donate/${event.short}"
    override fun getScheduleUrl(event: EventData): String = "https://oengus.io/marathon/${event.short.lowercase()}/schedule" // this is so icky
}

suspend fun Event.horaroSchedule(): FullSchedule? {
    if (horaroEvent == null || horaroSchedule == null) return null
    return Horaro.getSchedule(horaroEvent!!, horaroSchedule!!)
}

val excludedGameTitles = listOf("bonus game", "daily recap", "tasbot plays")

@Deprecated(message = "move somewhere else idk", level = DeprecationLevel.ERROR)
fun RunData.loadSrcGame(overrides: RunOverrides?) {
    src = if (overrides?.src == "")
        null
    else if (overrides?.src != null)
        overrides.src
    else run {
        val gameName = when {
            twitchName.isNotEmpty() -> twitchName
            displayName.isNotEmpty() -> displayName
            else -> name
        }
        excludedGameTitles.forEach { if (gameName.contains(it, true)) return@run null }
        srcDb.getGame(gameName).abbreviation
    }
}

class EventDataCacher(private val marathon: Marathon, private val skipLoad: Boolean) : Hook<Event> {
    override fun handle(item: Wrapper<Event>) {
        if (skipLoad) {
            item.value.skipLoad()
            return
        }
        val overrides = marathon.db.getOrCreateEventOverrides(item)
        if (overrides.startedAt != null)
            marathon.api.eventStartedAt[item.id] = overrides.startedAt
        if (overrides.endedAt != null) {
            marathon.api.eventEndedAt[item.id] = overrides.endedAt
            marathon.api.eventEndedAtExpiration.remove(item.id)
        }
    }
}

class EventOverrideUpdater(private val marathon: Marathon) : Hook<Event> {
    override fun handle(item: Wrapper<Event>) {
        marathon.eventIdCache[item.value.short] = item.id
        val overrides = marathon.db.getOrCreateEventOverrides(item)
        val now = Instant.now()
        if (overrides.startedAt == null
            && item.value.startTime != null
            && item.value.startTime!!.isBefore(now)
            )
            overrides.startedAt = item.value.startTime
        if (overrides.startedAt != null
            && overrides.endedAt == null
            && item.value.endTime != null
            && overrides.startedAt!! < item.value.endTime!!
            && item.value.endTime!!.plus(Duration.ofHours(1)).isBefore(now)
            )
            overrides.endedAt = item.value.endTime
        marathon.db.events.update(overrides)
    }
}
