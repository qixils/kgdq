package dev.qixils.gdq

import dev.qixils.gdq.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
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
    apiPath: String = "https://gamesdonequick.com/tracker/search/",
    val organization: String = "gdq",
) {
    private val logger = LoggerFactory.getLogger(GDQ::class.java)
    val apiPath: String
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()
    private val modelCache: MutableMap<Pair<ModelType<*>, Int>, Pair<Wrapper<*>, Instant>> = mutableMapOf()
    private val responseCache: MutableMap<String, Pair<List<Wrapper<*>>, Instant>> = mutableMapOf()
    protected var lastCachedRunners: Instant? = null
    val eventStartedAt = mutableMapOf<Int, Instant?>()
    val eventEndedAt = mutableMapOf<Int, Instant?>()
    val eventEndedAtExpiration = mutableMapOf<Int, Instant>()

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

    suspend fun updateEvent(id: Int) {
        handleEventExpiration(id)
        if (id in eventStartedAt || id in eventEndedAt) return
        val runs = getRuns(event = id).sortedBy { it.value.order }
        updateEvent(id, runs.firstOrNull()?.value?.startTime, runs.firstOrNull()?.value?.endTime)
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

    protected open suspend fun cacheRunners() { // TODO: remove `protected open` when ESA fixes their API bug
        // only cache runners every few hours
        val now = Instant.now()
        if (lastCachedRunners != null && lastCachedRunners!!.plus(ModelType.RUNNER.cacheFor).isAfter(now))
            return
        lastCachedRunners = now

        // cache runners
        var offset = 0
        var runners: List<Wrapper<Runner>>
        do {
            runners = getRunners(offset = offset)
            offset += runners.size
        } while (runners.isNotEmpty())
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

        // load data
        coroutineScope {
            models.forEach {
                preLoad?.handle(it)
                it.value.loadData(this@GDQ, it.id)
                postLoad?.let { pl -> launch { pl.handle(it) } } // run post-load in background
            }
        }

        // remove invalid models
        models.removeIf { !it.value.isValid() }

        // cache models
        val now = Instant.now()
        models.forEach { modelCache[it.modelType to it.id] = it to now }
        responseCache[query] = models to now

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
    suspend fun <M : Model> query(
        query: Query<M>,
        preLoad: Hook<M>? = null,
        postLoad: Hook<M>? = null,
    ): List<Wrapper<M>> = coroutineScope {
        // ensure runners are cached (they're high in quantity but basically fixed)
        val runnerCacheJob = launch { if (query.type == ModelType.RUNNER) cacheRunners() }

        // load from cache if possible
        var output: List<Wrapper<M>> = emptyList()
        if (query.id != null) {
            val pair = query.type to query.id
            if (modelCache.containsKey(pair)) {
                val (wrapper, cachedAt) = modelCache[pair]!!
                @Suppress("UNCHECKED_CAST") // the type is correct it's ok
                output = listOf(wrapper) as List<Wrapper<M>>

                // return cached data if it hasn't expired, otherwise clear the output variable if strict caching is enabled
                if (cachedAt.plus(query.type.cacheFor).isAfter(Instant.now())) {
                    return@coroutineScope output
                } else if (query.type.strictCache) {
                    output = emptyList()
                }
            }
        }

        // if output is not empty, then return it and allow cache to update in the background
        if (output.isNotEmpty())
            return@coroutineScope output

        // wait for runners to be cached
        runnerCacheJob.join()

        // return the result of the query
        return@coroutineScope query(query.asQueryString(), query.type, query.type.serializer, preLoad, postLoad)
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
        offset: Int? = null,
        preLoad: Hook<M>? = null,
        postLoad: Hook<M>? = null,
    ): List<Wrapper<M>> {
        return query(Query(type, id, event, runner, run, offset), preLoad, postLoad)
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
     * @param run      optional: the run to search for bids for
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
}