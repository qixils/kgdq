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

/**
 * The central class for performing requests to an instance of the GDQ donation tracker.
 */
@Suppress("HttpUrlsUsage")
class GDQ(apiPath: String = "https://gamesdonequick.com/tracker/search/") {
    private val apiPath: String
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

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
        val request = HttpRequest.newBuilder(URI.create("$apiPath?$query")).GET().build()
        val body = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await().body()
        val models = Json.decodeFromString(ListSerializer(Wrapper.serializer(modelSerializer)), body)
        models.forEach { it.value.loadData(this) }
        return models
    }

    /**
     * Performs a search on the GDQ tracker for the provided [query].
     *
     * @param query the query to search for
     * @return a list of models matching the query
     */
    suspend fun <M : Model> query(
        type: ModelType<M>,
        id: Int? = null,
        event: Int? = null,
        runner: Int? = null,
        run: Int? = null,
    ): List<Wrapper<M>> {
        // TODO: caching
        // create query string
        val params = mutableListOf("type=${type.id}")
        if (id != null) params.add("id=${id}")
        if (event != null) params.add("event=${event}")
        if (runner != null) params.add("runner=${runner}")
        if (run != null) params.add("run=${run}")
        val query = params.joinToString("&")
        // perform query
        return query(query, type.serializer)
    }
}