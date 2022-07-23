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

/**
 * The central class for performing requests to an instance of the GDQ donation tracker.
 */
@Suppress("HttpUrlsUsage")
class GDQ(apiPath: String = "https://gamesdonequick.com/tracker/search/") {
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val apiPath: String
    private val runners: MutableMap<Int, Wrapper<Runner>> = mutableMapOf()

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
    suspend fun <T : Model> query(query: String, modelSerializer: KSerializer<T>): List<Wrapper<T>> {
        val request = HttpRequest.newBuilder(URI.create("$apiPath?$query")).GET().build()
        val body = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await().body()
        return Json.decodeFromString(ListSerializer(Wrapper.serializer(modelSerializer)), body)
    }

    /**
     * Searches for the runner with the provided ID.
     *
     * @param id the ID of the runner to search for
     * @return the runner
     */
    suspend fun getRunner(id: Int): Runner {
        if (!runners.containsKey(id)) {
            val runner = query("type=runner&id=$id", Runner.serializer()).firstOrNull()
            if (runner != null)
                runners[id] = runner
            else
                throw IllegalArgumentException("Runner with ID $id could not be found.")
        }
        return runners[id]!!.value
    }
}