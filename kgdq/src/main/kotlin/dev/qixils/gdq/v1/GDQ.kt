package dev.qixils.gdq.v1

import dev.qixils.gdq.v1.models.*
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

/**
 * The central class for performing requests to an instance of the GDQ donation tracker.
 */
@Suppress("HttpUrlsUsage")
@Deprecated(message = "Use v2 API")
open class GDQ(
    /**
     * The base URL of the GDQ donation tracker API.
     */
    apiPath: String = "https://tracker.gamesdonequick.com/tracker/search/",
    /**
     * The types of models supported by this instance of the donation tracker.
     */
    private val supportedModels: Set<ModelType<*>> = ModelType.ALL,
    private val supportsPagination: Boolean = true,
) {
    val apiPath: String
    private val logger = LoggerFactory.getLogger(GDQ::class.java)
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()

    /**
     * The maximum number of items to return in a single request.
     * Discovered automatically by [getAllRunners].
     */
    private var limit: Int? = null

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
     * @return a list of models matching the query
     */
    private suspend fun <M : Model> query(
        query: String,
        modelType: ModelType<M>,
        modelSerializer: KSerializer<M>,
    ): List<M> {
        if (!supportedModels.contains(modelType))
            return emptyList()

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
        models.forEach { it.value.init(this, it.id) }

        // remove invalid models
        models.removeIf { !it.value.isValid() }

        // return
        return models.map { it.value }
    }

    /**
     * Performs a search on the GDQ tracker for the provided [query].
     *
     * @param query    the query to search for
     * @return a list of models matching the query
     */
    suspend fun <M : Model> query(
        query: Query<M>,
    ): List<M> {
        return query(query.asQueryString(), query.type, query.type.serializer)
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
    ): List<M> {
        return query(Query(type=type, id=id, event=event, runner=runner, run=run, name=name, offset=offset))
    }

    /**
     * Gets an object by its ID.
     *
     * @param type the type of object to get
     * @param id the ID of the object to get
     * @return the object, or null if it doesn't exist
     */
    suspend fun <M : Model> get(
        type: ModelType<M>,
        id: Int,
    ): M? {
        return query(type, id).firstOrNull()
    }

    /**
     * Gets an [Event] by its ID.
     *
     * @param id the ID of the event to get
     * @return the event, or null if it doesn't exist
     */
    suspend fun getEvent(
        id: Int,
    ): Event? {
        return get(ModelType.EVENT, id)
    }

    /**
     * Gets a [Runner] by their ID.
     *
     * @param id the ID of the runner to get
     * @return the runner, or null if it doesn't exist
     */
    suspend fun getRunner(
        id: Int,
    ): Runner? {
        return get(ModelType.RUNNER, id)
    }

    /**
     * Gets a [Run] by its ID.
     *
     * @param id the ID of the run to get
     * @return the run, or null if it doesn't exist
     */
    suspend fun getRun(
        id: Int,
    ): Run? {
        return get(ModelType.RUN, id)
    }

    /**
     * Gets a [Bid] by its ID.
     *
     * @param id the ID of the bid to get
     * @return the bid, or null if it doesn't exist
     */
    suspend fun getBid(
        id: Int,
    ): Bid? {
        return get(ModelType.ALL_BIDS, id)
    }

    /**
     * Gets a [Bid Parent][ModelType.BID] by its ID.
     *
     * @param id the ID of the bid parent to get
     * @return the bid parent, or null if it doesn't exist
     */
    suspend fun getBidParent(
        id: Int,
    ): Bid? {
        return get(ModelType.BID, id)
    }

    /**
     * Gets a [Bid Target][ModelType.BID_TARGET] by its ID.
     *
     * @param id the ID of the bid target to get
     * @return the bid target, or null if it doesn't exist
     */
    suspend fun getBidTarget(
        id: Int,
    ): Bid? {
        return get(ModelType.BID_TARGET, id)
    }

    /**
     * Gets a [Headset] by its ID.
     *
     * @param id the ID of the headset to get
     * @return the headset, or null if it doesn't exist
     */
    suspend fun getHeadset(
        id: Int,
    ): Headset? {
        return get(ModelType.HEADSET, id)
    }

    /**
     * Searches for [Event]s.
     *
     * @param offset   optional: the offset to start at
     * @return a list of events matching the search query
     */
    suspend fun getEvents(
        offset: Int? = null,
    ): List<Event> {
        return query(ModelType.EVENT, offset = offset)
    }

    /**
     * Searches for [Run]s.
     *
     * @param event    optional: the event to search for runs in
     * @param runner   optional: the runner to search for runs by
     * @param offset   optional: the offset to start at
     * @return a list of runs matching the search query
     */
    suspend fun getRuns(
        event: Int? = null,
        runner: Int? = null,
        offset: Int? = null,
    ): List<Run> {
        return query(ModelType.RUN, event = event, runner = runner, offset = offset)
    }

    /**
     * Searches for [Runner]s.
     *
     * @param event    optional: the event to search for runners in
     * @param offset   optional: the offset to start at
     * @return a list of runners matching the search query
     */
    suspend fun getRunners(
        event: Int? = null,
        offset: Int? = null,
    ): List<Runner> {
        return query(ModelType.RUNNER, event = event, offset = offset)
    }

    /**
     * Attempts to get all [Runner]s.
     *
     * @param event optional: the event to search for runners in
     * @return a list of runners matching the search query
     */
    suspend fun getAllRunners(
        event: Int? = null,
    ): List<Runner> {
        val runners = query(ModelType.RUNNER, event = event).toMutableList()
        if (!supportsPagination)
            return runners
        while (limit == null || runners.size % limit!! == 0) {
            val moreRunners = query(ModelType.RUNNER, event = event, offset = runners.size)
            if (moreRunners.isEmpty())
                break
            if (limit == null)
                limit = runners.size
            runners.addAll(moreRunners)
        }
        return runners
    }

    /**
     * Searches for [Bid]s.
     *
     * @param event    optional: the event to search for bids in
     * @param run      optional: the run to search for bids in
     * @param offset   optional: the offset to start at
     * @return a list of bids matching the search query
     */
    suspend fun getBids(
        event: Int? = null,
        run: Int? = null,
        offset: Int? = null,
    ): List<Bid> {
        return query(ModelType.ALL_BIDS, event = event, run = run, offset = offset)
    }

    /**
     * Searches for [Bid Parent][ModelType.BID]s.
     *
     * @param event    optional: the event to search for bid parents in
     * @param run      optional: the run to search for bid parents for
     * @param offset   optional: the offset to start at
     * @return a list of bid parents matching the search query
     */
    suspend fun getBidParents(
        event: Int? = null,
        run: Int? = null,
        offset: Int? = null,
    ): List<Bid> {
        return query(ModelType.BID, event = event, run = run, offset = offset)
    }

    /**
     * Searches for [Bid Target][ModelType.BID_TARGET]s.
     *
     * @param event    optional: the event to search for bids in
     * @param run      optional: the run to search for bids for
     * @param offset   optional: the offset to start at
     * @return a list of bids matching the search query
     */
    suspend fun getBidTargets(
        event: Int? = null,
        run: Int? = null,
        offset: Int? = null,
    ): List<Bid> {
        return query(ModelType.BID_TARGET, event = event, run = run, offset = offset)
    }

    /**
     * Searches for [Headset]s.
     *
     * @param name     optional: the name to search for headsets by
     * @param offset   optional: the offset to start at
     * @return a list of headsets matching the search query
     */
    suspend fun getHeadsets(
        name: String? = null,
        offset: Int? = null,
    ): List<Headset> {
        return query(ModelType.HEADSET, name = name, offset = offset)
    }
}

/**
 * A derivative of [GDQ] tailored for ESA.
 */
open class ESA(
    apiPath: String = "https://donations.esamarathon.com/search/"
) : GDQ(apiPath, ModelType.ALL.minus(ModelType.HEADSET), false)

class HEK : ESA("https://hekathon.esamarathon.com/search/")
class RPGLB : GDQ("https://tracker.rpglimitbreak.com/search/", ModelType.ALL.minus(ModelType.HEADSET))
class BSG : GDQ("https://tracker.bsgmarathon.com/tracker/search/") // shockingly using a fresh fork of the GDQ tracker instead of ESA's ancient fork