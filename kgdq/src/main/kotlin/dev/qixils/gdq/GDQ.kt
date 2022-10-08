package dev.qixils.gdq

import dev.qixils.gdq.models.Model
import dev.qixils.gdq.models.Runner
import dev.qixils.gdq.models.Wrapper
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
    ): List<Wrapper<M>> = coroutineScope {
        // look in the cache first
        if (responseCache.containsKey(query)) {
            val (wrappers, cachedAt) = responseCache[query]!!
            if (cachedAt.plus(modelType.cacheFor).isAfter(Instant.now())) {
                @Suppress("UNCHECKED_CAST") // the type is correct it's ok
                return@coroutineScope wrappers as List<Wrapper<M>>
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
        models.forEach {
            preLoad?.handle(it)
            it.value.loadData(this@GDQ, it.id)
            launch(Job()) { postLoad?.handle(it) } // run post-load in background
        }

        // remove invalid models
        models.removeIf { !it.value.isValid() }

        // cache models
        val now = Instant.now()
        models.forEach { modelCache[it.modelType to it.id] = it to now }
        responseCache[query] = models to now

        // return
        return@coroutineScope models
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

        // create lazy query
        val queryResult = async(Job(), start = CoroutineStart.LAZY) {
            runnerCacheJob.join() // wait for runners to be cached
            query(query.asQueryString(), query.type, query.type.serializer, preLoad, postLoad)
        }
        var output: List<Wrapper<M>> = emptyList()

        // load from cache if possible
        if (query.id != null) {
            val pair = query.type to query.id
            if (modelCache.containsKey(pair)) {
                val (wrapper, cachedAt) = modelCache[pair]!!
                @Suppress("UNCHECKED_CAST") // the type is correct it's ok
                output = listOf(wrapper) as List<Wrapper<M>>

                // return cached data if it hasn't expired, otherwise clear the output variable if strict caching is enabled
                if (cachedAt.plus(query.type.cacheFor).isAfter(Instant.now())) {
                    queryResult.cancel()
                    return@coroutineScope output
                } else if (query.type.strictCache) {
                    output = emptyList()
                }
            }
        }

        // begin load from API
        queryResult.start()

        // if output is not empty, then return it and allow cache to update in the background
        if (output.isNotEmpty())
            return@coroutineScope output

        // return the result of the query
        return@coroutineScope queryResult.await()
    }

    /**
     * Performs a search on the GDQ tracker for the provided query
     *
     * @param type     the type of model being queried for
     * @param id       optional: the id of the model to get
     * @param event    optional: the event to query from
     * @param runner   optional: the runner to query from
     * @param run      optional: the run to query from
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
            runners = query(type = ModelType.RUNNER, offset = offset)
            offset += runners.size
        } while (runners.isNotEmpty())
    }
}