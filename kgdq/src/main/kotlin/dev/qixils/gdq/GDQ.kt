package dev.qixils.gdq

import dev.qixils.gdq.models.Model
import dev.qixils.gdq.models.Runner
import dev.qixils.gdq.models.Wrapper
import kotlinx.coroutines.future.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.logging.Logger

/**
 * The central class for performing requests to an instance of the GDQ donation tracker.
 */
@Suppress("HttpUrlsUsage")
open class GDQ(apiPath: String = "https://gamesdonequick.com/tracker/search/") {
    private val logger = Logger.getLogger("GDQ")
    val apiPath: String
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()
    private val cache: MutableMap<Pair<ModelType<*>, Int>, Pair<Wrapper<*>, Instant>> = mutableMapOf()
    private var lastCachedRunners: Instant? = null

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
     * @param modelSerializer the serializer of model being queried for
     * @return a list of models matching the query
     */
    suspend fun <M : Model> query(query: String, modelSerializer: KSerializer<M>): List<Wrapper<M>> {
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
        models.forEach { it.value.loadData(this, it.id) }

        // remove invalid models
        models.removeIf{ !it.value.isValid() }

        // cache models
        models.forEach { cache[it.modelType to it.id] = it to Instant.now() }

        // return
        return models
    }

    /**
     * Performs a search on the GDQ tracker for the provided query
     *
     * @param type   the type of model being queried for
     * @param id     optional: the id of the model to get
     * @param event  optional: the event to query from
     * @param runner optional: the runner to query from
     * @param run    optional: the run to query from
     * @return a list of models matching the query
     */
    suspend fun <M : Model> query(
        type: ModelType<M>,
        id: Int? = null,
        event: Int? = null,
        runner: Int? = null,
        run: Int? = null,
        offset: Int? = null, // TODO: send PR to ESA fixing this param (edit https://github.com/ESAMarathon/donation-tracker/blob/esa/views/api.py#L165-L166 using https://github.com/GamesDoneQuick/donation-tracker/blob/master/tracker/views/api.py#L384-L395)
    ): List<Wrapper<M>> {
        // ensure runners are cached (they're high in quantity but basically fixed)
        if (type == ModelType.RUNNER) cacheRunners()

        // load from cache if possible
        if (id != null) {
            val pair = type to id
            if (cache.containsKey(pair)) {
                val (wrapper, cachedAt) = cache[pair]!!
                if (cachedAt.plus(type.cacheFor).isAfter(Instant.now())) {
                    @Suppress("UNCHECKED_CAST") // the type is correct it's ok
                    return listOf(wrapper) as List<Wrapper<M>>
                }
            }
        }

        // create query string
        val params = mutableListOf("type=${type.id}")
        if (id != null) params.add("id=${id}")
        if (event != null) params.add("event=${event}")
        if (runner != null) params.add("runner=${runner}")
        if (run != null) params.add("run=${run}")
        if (offset != null) params.add("offset=${offset}")
        val query = params.joinToString("&")

        // perform query
        return query(query, type.serializer)
    }

    protected open suspend fun cacheRunners() { // TODO: remove `open` when ESA fixes their API bug
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