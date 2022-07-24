package dev.qixils.gdq

import dev.qixils.gdq.models.Model
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
class GDQ(apiPath: String = "https://gamesdonequick.com/tracker/search/") {
    private val logger = Logger.getLogger("GDQ")
    private val apiPath: String
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val cache: MutableMap<Pair<ModelType<*>, Int>, Pair<Wrapper<*>, Instant>> = mutableMapOf()

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
        val url = "$apiPath?$query"
        logger.info("Querying $url")
        val uri = URI.create(url)
        val request = HttpRequest.newBuilder(uri).GET().build()
        val body = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await().body()
        val models = json.decodeFromString(ListSerializer(Wrapper.serializer(modelSerializer)), body)
        models.forEach {
            it.value.loadData(this) // initialize model's data
            cache[it.modelType to it.id] = it to Instant.now() // cache model
        }
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
        offset: Int? = null,
    ): List<Wrapper<M>> {
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
}