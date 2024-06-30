package dev.qixils.gdq

import dev.qixils.gdq.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.lang.Integer.max
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

/**
 * The central class for performing requests to an instance of the GDQ donation tracker.
 */
@Suppress("HttpUrlsUsage")
open class GDQ(
    /**
     * The base URL of the GDQ donation tracker API.
     */
    apiPath: String = "https://tracker.gamesdonequick.com/tracker/search/",
    /**
     * The types of models supported by this instance of the donation tracker.
     */
    val supportedModels: Set<ModelType<*>> = ModelType.ALL,
) {
    private val logger = LoggerFactory.getLogger(GDQ::class.java)
    val apiPath: String
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()
    private val modelCache: MutableMap<Pair<CacheType, Int>, Pair<Wrapper<*>, Instant>> = mutableMapOf()
    private val responseCache: MutableMap<String, Pair<List<Wrapper<*>>, Instant>> = mutableMapOf()
    protected var lastCachedRunners: Instant? = null
    protected var lastCachedHeadset: Instant? = null
    val eventStartedAt = mutableMapOf<Int, Instant?>()
    val eventEndedAt = mutableMapOf<Int, Instant?>()
    val eventEndedAtExpiration = mutableMapOf<Int, Instant>()

    /**
     * The maximum number of items to return in a single request.
     * Discovered automatically by [cacheRunners].
     */
    private var limit: Int? = null

    fun handleEventExpiration(id: Int) {
        if (id !in eventEndedAtExpiration) return
        if (eventEndedAtExpiration[id]!!.isBefore(Instant.now())) {
            eventEndedAtExpiration.remove(id)
            eventEndedAt.remove(id)
        }
    }

    fun updateEvent(id: Int, startedAt: Instant?, endedAt: Instant?) {
        eventStartedAt[id] = startedAt
        eventEndedAt[id] = endedAt
        if (startedAt == null || endedAt == null) return
        val expires = Instant.now()
            .coerceAtLeast(startedAt)
            .plus(Duration.ofDays(1))
            .coerceAtMost(endedAt.plus(Duration.ofHours(1)))
        if (expires.isAfter(Instant.now()))
            eventEndedAtExpiration[id] = expires
        else
            eventEndedAtExpiration.remove(id)
    }

    suspend fun updateEvent(id: Int, skipLoad: Boolean = false) {
        handleEventExpiration(id)
        if (skipLoad) return
        if (id in eventStartedAt && id in eventEndedAt) return
        val runs = getRuns(event = id).sortedBy { it.value.order }
        updateEvent(id, runs.firstOrNull()?.value?.startTime, runs.lastOrNull()?.value?.endTime)
    }

    /**
     * Constructs a new GDQ instance with the provided API path.
     */
    init {
        var path: String = apiPath
        if (path.startsWith("http://"))
            path = apiPath.replaceFirst("http://", "https://")
        if (!path.startsWith("https://"))
            path = "https://$path"
        if (!path.endsWith("/"))
            path += "/"
        this.apiPath = path
    }

    protected open suspend fun cacheRunners() {
        if (!supportedModels.contains(ModelType.RUNNER))
            return

        // only cache runners every few hours
        val now = Instant.now()
        if (lastCachedRunners != null && lastCachedRunners!!.plus(CacheType.RUNNER.duration).isAfter(now))
            return
        lastCachedRunners = now

        // cache runners
        var offset = 0
        do {
            val runners = getRunners(offset = offset)
            offset += runners.size
            if (runners.isEmpty())
                break
            if (limit != null && runners.size < limit!!)
                break
            limit = max(limit ?: 0, runners.size)
        } while (true)
    }

    protected open suspend fun cacheHeadsets() {
        if (!supportedModels.contains(ModelType.HEADSET))
            return

        // only cache headsets every few hours
        val now = Instant.now()
        if (lastCachedHeadset != null && lastCachedHeadset!!.plus(CacheType.HEADSET.duration).isAfter(now))
            return
        lastCachedHeadset = now

        // cache headsets
        var offset = 0
        do {
            val headsets = getHeadsets(offset = offset)
            offset += headsets.size
            if (headsets.isEmpty())
                break
            if (limit != null && headsets.size < limit!!)
                break
            limit = max(limit ?: 0, headsets.size)
        } while (true)
    }

    suspend fun cacheAll() {
        cacheRunners()
        cacheHeadsets()
    }

    /**
     * Performs a search on the GDQ tracker for the provided [query].
     *
     * @param query           the query to search for
     * @param modelType       the type of model to return
     * @param modelSerializer the serializer of model being queried for
     * @param preLoad         a hook to run before a model is loaded
     * @param postLoad        a hook to run after a model is loaded
     * @return a list of models matching the query
     */
    private suspend fun <M : Model> query(
        query: String,
        modelType: ModelType<M>,
        modelSerializer: KSerializer<M>,
        preLoad: Hook<M>? = null,
        postLoad: Hook<M>? = null,
    ): List<Wrapper<M>> {
        // look in the cache first
        if (responseCache.containsKey(query)) {
            val (wrappers, cachedAt) = responseCache[query]!!
            if (cachedAt.plus(modelType.cacheFor).isAfter(Instant.now())) {
                @Suppress("UNCHECKED_CAST") // the type is correct it's ok
                return wrappers as List<Wrapper<M>>
            }
        }

        // logging
        val url = "$apiPath?$query"
        logger.info("Querying $url")
        val uri = URI.create(url)

        // perform request
        val request = HttpRequest.newBuilder(uri).GET().build()
        val body = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await().body()

        // deserialize | TODO: handle deserializing errors (error: String, exception: String)
        val models = json
            .decodeFromString(ListSerializer(Wrapper.serializer(modelSerializer)), body)
            .toMutableList()
        val jobs = mutableListOf<Job>()

        // load data
        coroutineScope {
            models.forEach {
                preLoad?.handle(it)
                it.value.loadData(this@GDQ, it.id)
                postLoad?.let { pl -> jobs.add(launch { pl.handle(it) }) } // run post-load in background
            }
        }
        jobs.joinAll()

        // remove invalid models
        models.removeIf { !it.value.isValid() }

        // cache models
        if (models.none { it.value.skippedLoad }) {
            val now = Instant.now()
            models.forEach { modelCache[it.modelType.cacheType to it.id] = it to now }
            responseCache[query] = models to now
        }

        // return
        return models
    }

    /**
     * Performs a search on the GDQ tracker for the provided [query].
     *
     * @param query    the query to search for
     * @param preLoad  a hook to run before a model is loaded
     * @param postLoad a hook to run after a model is loaded
     * @return a list of models matching the query
     */
    @OptIn(DelicateCoroutinesApi::class) // it's a little janky but it should be ok
    suspend fun <M : Model> query(
        query: Query<M>,
        preLoad: Hook<M>? = null,
        postLoad: Hook<M>? = null,
    ): List<Wrapper<M>> {
        if (!supportedModels.contains(query.type))
            return emptyList()

        // cache models that are large in quantity but don't change often
        when (query.type.cacheType) {
            CacheType.RUNNER -> GlobalScope.launch { cacheRunners() }
            CacheType.HEADSET -> GlobalScope.launch { cacheHeadsets() }
        }

        // load from cache if possible
        val output: List<Wrapper<M>>
        if (query.id != null) {
            val pair = query.type.cacheType to query.id
            if (modelCache.containsKey(pair)) {
                val (wrapper, cachedAt) = modelCache[pair]!!
                @Suppress("UNCHECKED_CAST") // the type is correct it's ok
                output = listOf(wrapper) as List<Wrapper<M>>

                // return cached data if it hasn't expired or if the cache is not strict
                if (cachedAt.plus(query.type.cacheFor).isAfter(Instant.now()) || !query.type.strictCache) {
                    return output
                }
            }
        }

        // return the result of the query
        return query(query.asQueryString(), query.type, query.type.serializer, preLoad, postLoad)
    }

    /**
     * Performs a search on the GDQ tracker for the provided query.
     *
     * @param type     the type of model being queried for
     * @param id       optional: the id of the model to get
     * @param event    optional: the event to query from
     * @param runner   optional: the runner to query from
     * @param run      optional: the run to query from
     * @param offset   optional: the offset to start from
     * @param preLoad  a hook to run before a model is loaded
     * @param postLoad a hook to run after a model is loaded
     * @return a list of models matching the query
     */
    suspend fun <M : Model> query(
        type: ModelType<M>,
        id: Int? = null,
        event: Int? = null,
        runner: Int? = null,
        run: Int? = null,
        name: String? = null,
        offset: Int? = null,
        preLoad: Hook<M>? = null,
        postLoad: Hook<M>? = null,
    ): List<Wrapper<M>> {
        return query(Query(type=type, id=id, event=event, runner=runner, run=run, name=name, offset=offset), preLoad, postLoad)
    }

    /**
     * Gets an object by its ID.
     *
     * @param type the type of object to get
     * @param id the ID of the object to get
     * @param preLoad a hook to run before the object is loaded
     * @param postLoad a hook to run after the object is loaded
     * @return the object, or null if it doesn't exist
     */
    suspend fun <M : Model> get(
        type: ModelType<M>,
        id: Int,
        preLoad: Hook<M>? = null,
        postLoad: Hook<M>? = null,
    ): Wrapper<M>? {
        return query(type, id, preLoad = preLoad, postLoad = postLoad).firstOrNull()
    }

    /**
     * Gets an [Event] by its ID.
     *
     * @param id the ID of the event to get
     * @param preLoad a hook to run before the event is loaded
     * @param postLoad a hook to run after the event is loaded
     * @return the event, or null if it doesn't exist
     */
    suspend fun getEvent(
        id: Int,
        preLoad: Hook<Event>? = null,
        postLoad: Hook<Event>? = null,
    ): Wrapper<Event>? {
        return get(ModelType.EVENT, id, preLoad, postLoad)
    }

    /**
     * Gets a [Runner] by their ID.
     *
     * @param id the ID of the runner to get
     * @param preLoad a hook to run before the runner is loaded
     * @param postLoad a hook to run after the runner is loaded
     * @return the runner, or null if it doesn't exist
     */
    suspend fun getRunner(
        id: Int,
        preLoad: Hook<Runner>? = null,
        postLoad: Hook<Runner>? = null,
    ): Wrapper<Runner>? {
        return get(ModelType.RUNNER, id, preLoad, postLoad)
    }

    /**
     * Gets a [Run] by its ID.
     *
     * @param id the ID of the run to get
     * @param preLoad a hook to run before the run is loaded
     * @param postLoad a hook to run after the run is loaded
     * @return the run, or null if it doesn't exist
     */
    suspend fun getRun(
        id: Int,
        preLoad: Hook<Run>? = null,
        postLoad: Hook<Run>? = null,
    ): Wrapper<Run>? {
        return get(ModelType.RUN, id, preLoad, postLoad)
    }

    /**
     * Gets a [Bid] by its ID.
     *
     * @param id the ID of the bid to get
     * @param preLoad a hook to run before the bid is loaded
     * @param postLoad a hook to run after the bid is loaded
     * @return the bid, or null if it doesn't exist
     */
    suspend fun getBid(
        id: Int,
        preLoad: Hook<Bid>? = null,
        postLoad: Hook<Bid>? = null,
    ): Wrapper<Bid>? {
        return get(ModelType.ALL_BIDS, id, preLoad, postLoad)
    }

    /**
     * Gets a [Bid Parent][ModelType.BID] by its ID.
     *
     * @param id the ID of the bid parent to get
     * @param preLoad a hook to run before the bid parent is loaded
     * @param postLoad a hook to run after the bid parent is loaded
     * @return the bid parent, or null if it doesn't exist
     */
    suspend fun getBidParent(
        id: Int,
        preLoad: Hook<Bid>? = null,
        postLoad: Hook<Bid>? = null,
    ): Wrapper<Bid>? {
        return get(ModelType.BID, id, preLoad, postLoad)
    }

    /**
     * Gets a [Bid Target][ModelType.BID_TARGET] by its ID.
     *
     * @param id the ID of the bid target to get
     * @param preLoad a hook to run before the bid target is loaded
     * @param postLoad a hook to run after the bid target is loaded
     * @return the bid target, or null if it doesn't exist
     */
    suspend fun getBidTarget(
        id: Int,
        preLoad: Hook<Bid>? = null,
        postLoad: Hook<Bid>? = null,
    ): Wrapper<Bid>? {
        return get(ModelType.BID_TARGET, id, preLoad, postLoad)
    }

    /**
     * Gets a [Headset] by its ID.
     *
     * @param id the ID of the headset to get
     * @param preLoad a hook to run before the headset is loaded
     * @param postLoad a hook to run after the headset is loaded
     * @return the headset, or null if it doesn't exist
     */
    suspend fun getHeadset(
        id: Int,
        preLoad: Hook<Headset>? = null,
        postLoad: Hook<Headset>? = null,
    ): Wrapper<Headset>? {
        return get(ModelType.HEADSET, id, preLoad, postLoad)
    }

    /**
     * Searches for [Event]s.
     *
     * @param offset   optional: the offset to start at
     * @param preLoad  optional: a hook to run before each event is loaded
     * @param postLoad optional: a hook to run after each event is loaded
     * @return a list of events matching the search query
     */
    suspend fun getEvents(
        offset: Int? = null,
        preLoad: Hook<Event>? = null,
        postLoad: Hook<Event>? = null,
    ): List<Wrapper<Event>> {
        return query(ModelType.EVENT, offset = offset, preLoad = preLoad, postLoad = postLoad)
    }

    /**
     * Searches for [Run]s.
     *
     * @param event    optional: the event to search for runs in
     * @param runner   optional: the runner to search for runs by
     * @param offset   optional: the offset to start at
     * @param preLoad  optional: a hook to run before each run is loaded
     * @param postLoad optional: a hook to run after each run is loaded
     * @return a list of runs matching the search query
     */
    suspend fun getRuns(
        event: Int? = null,
        runner: Int? = null,
        offset: Int? = null,
        preLoad: Hook<Run>? = null,
        postLoad: Hook<Run>? = null,
    ): List<Wrapper<Run>> {
        return query(ModelType.RUN, event = event, runner = runner, offset = offset, preLoad = preLoad, postLoad = postLoad)
    }

    /**
     * Searches for [Runner]s.
     *
     * @param event    optional: the event to search for runners in
     * @param offset   optional: the offset to start at
     * @param preLoad  optional: a hook to run before each runner is loaded
     * @param postLoad optional: a hook to run after each runner is loaded
     * @return a list of runners matching the search query
     */
    suspend fun getRunners(
        event: Int? = null,
        offset: Int? = null,
        preLoad: Hook<Runner>? = null,
        postLoad: Hook<Runner>? = null,
    ): List<Wrapper<Runner>> {
        return query(ModelType.RUNNER, event = event, offset = offset, preLoad = preLoad, postLoad = postLoad)
    }

    /**
     * Searches for [Bid]s.
     *
     * @param event    optional: the event to search for bids in
     * @param run      optional: the run to search for bids in
     * @param offset   optional: the offset to start at
     * @param preLoad  optional: a hook to run before each bid is loaded
     * @param postLoad optional: a hook to run after each bid is loaded
     * @return a list of bids matching the search query
     */
    suspend fun getBids(
        event: Int? = null,
        run: Int? = null,
        offset: Int? = null,
        preLoad: Hook<Bid>? = null,
        postLoad: Hook<Bid>? = null,
    ): List<Wrapper<Bid>> {
        return query(ModelType.ALL_BIDS, event = event, run = run, offset = offset, preLoad = preLoad, postLoad = postLoad)
    }

    /**
     * Searches for [Bid Parent][ModelType.BID]s.
     *
     * @param event    optional: the event to search for bid parents in
     * @param run      optional: the run to search for bid parents for
     * @param offset   optional: the offset to start at
     * @param preLoad  optional: a hook to run before each bid parent is loaded
     * @param postLoad optional: a hook to run after each bid parent is loaded
     * @return a list of bid parents matching the search query
     */
    suspend fun getBidParents(
        event: Int? = null,
        run: Int? = null,
        offset: Int? = null,
        preLoad: Hook<Bid>? = null,
        postLoad: Hook<Bid>? = null,
    ): List<Wrapper<Bid>> {
        return query(ModelType.BID, event = event, run = run, offset = offset, preLoad = preLoad, postLoad = postLoad)
    }

    /**
     * Searches for [Bid Target][ModelType.BID_TARGET]s.
     *
     * @param event    optional: the event to search for bids in
     * @param run      optional: the run to search for bids for
     * @param offset   optional: the offset to start at
     * @param preLoad  optional: a hook to run before each bid is loaded
     * @param postLoad optional: a hook to run after each bid is loaded
     * @return a list of bids matching the search query
     */
    suspend fun getBidTargets(
        event: Int? = null,
        run: Int? = null,
        offset: Int? = null,
        preLoad: Hook<Bid>? = null,
        postLoad: Hook<Bid>? = null,
    ): List<Wrapper<Bid>> {
        return query(ModelType.BID_TARGET, event = event, run = run, offset = offset, preLoad = preLoad, postLoad = postLoad)
    }

    /**
     * Searches for [Headset]s.
     *
     * @param name     optional: the name to search for headsets by
     * @param offset   optional: the offset to start at
     * @param preLoad  optional: a hook to run before each headset is loaded
     * @param postLoad optional: a hook to run after each headset is loaded
     * @return a list of headsets matching the search query
     */
    suspend fun getHeadsets(
        name: String? = null,
        offset: Int? = null,
        preLoad: Hook<Headset>? = null,
        postLoad: Hook<Headset>? = null,
    ): List<Wrapper<Headset>> {
        return query(ModelType.HEADSET, name = name, offset = offset, preLoad = preLoad, postLoad = postLoad)
    }
}

/**
 * A derivative of [GDQ] tailored for ESA.
 */
open class ESA(
    apiPath: String = "https://donations.esamarathon.com/search/"
) : GDQ(apiPath, ModelType.ALL.minus(ModelType.HEADSET)) {
    override suspend fun cacheRunners() {
        val now = Instant.now()
        if (lastCachedRunners != null && lastCachedRunners!!.plus(ModelType.RUNNER.cacheFor).isAfter(now))
            return
        lastCachedRunners = now
        getRunners()
    }
}

class HEK : ESA("https://hekathon.esamarathon.com/search/")
class RPGLB : GDQ("https://tracker.rpglimitbreak.com/search/", ModelType.ALL.minus(ModelType.HEADSET))
class BSG : GDQ("https://tracker.bsgmarathon.com/search/") // shockingly using a fresh fork of the GDQ tracker instead of ESA's ancient fork