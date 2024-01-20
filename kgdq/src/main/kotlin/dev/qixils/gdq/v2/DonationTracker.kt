package dev.qixils.gdq.v2

import SearchParamsBuilder
import buildSearchParams
import dev.qixils.gdq.v2.models.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync
import org.jetbrains.annotations.ApiStatus.Internal
import org.slf4j.LoggerFactory
import readBodyString
import toSearchParams

class DonationTracker(
    /**
     * The base URL of the donation tracker API.
     */
    apiPath: String = "https://gamesdonequick.com/tracker/api/v2/",
) {
    val apiPath: String
    private val logger = LoggerFactory.getLogger(DonationTracker::class.java)
    private val client = OkHttpClient()

    @Internal
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        serializersModule = SerializersModule {
        }
    }

    /**
     * Constructs a new DonationTracker instance with the provided API path.
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

    suspend fun <M : Model> get(
        url: HttpUrl,
        modelSerializer: KSerializer<M>,
    ): M? {
        // logging
        logger.info("Querying $url")

        // perform request
        val request = Request.Builder().url(url).get().build()
        val body = client.newCall(request).executeAsync().readBodyString() ?: run {
            logger.warn("Got null body on ${request.url}")
            return null
        }

        // deserialize | TODO: handle deserializing errors
        val page: M? = try {
            json.decodeFromString(modelSerializer, body)
        } catch (e: Exception) {
            logger.warn("Error decoding on ${request.url}", e)
            null
        }

        // return
        return page
    }

    suspend inline fun <reified M : Model> get(
        url: HttpUrl,
    ): M? {
        return get(url, json.serializersModule.serializer())
    }

    suspend fun <M : Model> getPage(
        url: HttpUrl,
        modelSerializer: KSerializer<M>,
    ): Page<M> {
        val serializer = Page.serializer(modelSerializer)
        return get(url, serializer) ?: Page<M>().apply { init(this@DonationTracker, serializer) }
    }

    suspend inline fun <reified M : Model> getPage(
        url: HttpUrl
    ): Page<M> {
        return getPage(url, json.serializersModule.serializer())
    }

    @Suppress("UNCHECKED_CAST")
    fun url(
        path: String,
        query: Any? = null
    ): HttpUrl {
        var url = apiPath + path
        when (query) {
            null -> {}
            is Map<*, *> -> url += (query as Map<String, String?>).toSearchParams()
            is SearchParamsBuilder -> url += query.build()
            else -> url += query.toString()
        }
        return url.toHttpUrl()
    }

    fun params(
        limit: Int? = null,
        offset: Int? = null,
    ): String = buildSearchParams {
        addIfNotNull("limit", limit?.toString())
        addIfNotNull("offset", offset?.toString())
    }

    suspend fun getEvents(): Page<Event> {
        return getPage(url("events/"))
    }

    suspend fun getEvent(
        id: Int,
    ): Event? {
        return get(url("events/${id}/"))
    }

    suspend fun getRun(
        id: Int
    ): Run? {
        return get(url("runs/$id/"))
    }

    suspend fun getRuns(
        limit: Int? = null,
        offset: Int? = null,
    ): Page<Run> {
        return getPage(url("runs/", params(limit, offset)))
    }

    suspend fun getEventRuns(
        event: Int,
    ): Page<Run> {
        return getPage(url("events/$event/runs/"))
    }

    suspend fun getRunner(
        id: Int
    ): Runner? {
        return get(url("runners/$id/"))
    }

    suspend fun getRunners(
        limit: Int? = null,
        offset: Int? = null,
    ): Page<Runner> {
        return getPage(url("runners/", params(limit, offset)))
    }

    suspend fun getEventRunners(
        event: Int,
    ): Page<Runner> {
        return getPage(url("events/$event/runners/"))
    }

    suspend fun getBid(
        id: Int
    ): Bid? {
        return get(url("bids/$id/"))
    }

    suspend fun getBids(
        limit: Int? = null,
        offset: Int? = null,
    ): Page<Bid> {
        return getPage(url("bids/", params(limit, offset)))
    }

}