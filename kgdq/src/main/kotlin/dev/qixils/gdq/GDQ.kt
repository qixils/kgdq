package dev.qixils.gdq

import dev.qixils.gdq.models.Model
import dev.qixils.gdq.models.Runner
import dev.qixils.gdq.models.Wrapper
import kotlinx.coroutines.future.await
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
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
     * @param query the query to search for
     * @param model the type of model being queried for
     * @return a list of models matching the query
     */
    suspend fun <T : Model> query(query: String, model: Class<T>): List<T> {
        val request = HttpRequest.newBuilder(URI.create("$apiPath?$query")).GET().build()
        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val body = response.body()
        return Json.decodeFromString(ListSerializer(Wrapper.serializer(model)), body)
    }

    /**
     * Searches for the runner with the provided ID.
     *
     * @param id the ID of the runner to search for
     * @return the runner
     */
    fun getRunner(id: Int): Runner {
        if (!runners.containsKey(id)) {

        }
    }
}