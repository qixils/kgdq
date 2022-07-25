@file:OptIn(InternalHoraroApi::class)

package dev.qixils.horaro

import dev.qixils.horaro.models.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.logging.Logger

/**
 * The main class of the async Horaro library which fetches read-only data from the
 * [Horaro API](https://horaro.org/-/api).
 */
sealed class Horaro {

    /**
     * The default instance of the library.
     */
    companion object Default : Horaro() {

        /**
         * Utility method for generating a parameter string.
         */
        private fun paramsToString(params: Map<String, String?>): String {
            val sb = StringBuilder()
            params.entries.forEach {
                if (it.value == null) return@forEach
                if (sb.isEmpty()) sb.append("?") else sb.append("&")
                sb.append(it.key).append('=').append(it.value)
            }
            return sb.toString()
        }

        /**
         * Utility method for generating a parameter string.
         */
        private fun paramToString(key: String, value: String?): String {
            if (value == null) return ""
            return "?$key=$value"
        }
    }

    private val logger = Logger.getLogger("Horaro")
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private suspend fun get(url: String): String {
        // logging
        logger.info("Querying $url")
        val uri = URI.create(url)

        // request
        val request = HttpRequest.newBuilder(uri).GET().build()
        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val status = response.statusCode()
        if (status != 200)
            throw StatusCodeException(status)
        return response.body()
    }

    /**
     * Fetches data from the provided [url] and decodes it using the provided [serializer].
     * Intended for internal use only.
     *
     * @param url        the URL to fetch data from
     * @param serializer the serializer to use for decoding the data
     * @return the decoded data
     */
    @InternalHoraroApi
    suspend fun <M> get(url: String, serializer: KSerializer<M>): M {
        return json.decodeFromString(serializer, get(url))
    }

    private suspend fun <M> getOrNull(url: String, serializer: KSerializer<out Response<M>>): M? {
        return try {
            get(url, serializer).data
        } catch (e: StatusCodeException) {
            if (e.statusCode == 404)
                null
            else
                throw e
        }
    }

    /**
     * Retrieves a paginated list of all events.
     * Optionally filters the result to only events whose name contains the given string.
     *
     * @param name   the name to search for
     * @param offset the offset of the list
     * @return a [ListResponse] of [Event]s
     */
    suspend fun getEvents(name: String? = null, offset: Int = 0): EventResponse {
        if (offset < 0) throw IllegalArgumentException("Offset must be >= 0")
        val serializer = EventResponse.serializer()
        val params = paramsToString(mapOf("offset" to offset.toString(), "name" to name))
        val url = "https://horaro.org/-/api/v1/events$params"
        return get(url, serializer)
    }

    /**
     * Retrieves a specific event given its unique ID or slug.
     *
     * @param id the ID or slug of the event
     * @return the [Event] if found, null otherwise
     */
    suspend fun getEvent(id: String): Event? {
        val serializer = ResponseImpl.serializer(Event.serializer())
        val url = "https://horaro.org/-/api/v1/events/$id"
        return getOrNull(url, serializer)
    }

    /**
     * Retrieves a paginated list of schedules for the given [event].
     *
     * @param event  the ID or slug of the event to retrieve the schedules for
     * @param offset the offset of the list
     * @return a [ListResponse] of [FullSchedule]s
     * @throws IllegalArgumentException if the event is not found
     */
    suspend fun getSchedules(event: String, offset: Int = 0): ScheduleResponse {
        if (offset < 0) throw IllegalArgumentException("Offset must be >= 0")
        val serializer = ScheduleResponse.serializer()
        val params = paramToString("offset", offset.toString())
        val url = "https://horaro.org/-/api/v1/events/$event/schedules$params"
        try {
            return get(url, serializer)
        } catch (e: StatusCodeException) {
            if (e.statusCode == 404)
                throw IllegalArgumentException("Event '$event' not found")
            else
                throw e
        }
    }

    /**
     * Retrieves a paginated list of schedules for the given [event].
     *
     * @param event  the event to retrieve the schedules for
     * @param offset the offset of the list
     * @return a [ListResponse] of [FullSchedule]s
     * @throws IllegalArgumentException if the event is not found
     */
    suspend fun getSchedules(event: Event, offset: Int = 0): ScheduleResponse {
        return getSchedules(event.id, offset)
    }

    /**
     * Retrieves a specific schedule given its unique ID.
     *
     * @param id        the ID of the schedule
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the [FullSchedule] if found, null otherwise
     */
    suspend fun getSchedule(id: String, hiddenKey: String? = null): FullSchedule? {
        val serializer = ResponseImpl.serializer(FullSchedule.serializer())
        val params = paramToString("hiddenkey", hiddenKey)
        val url = "https://horaro.org/-/api/v1/schedules/$id$params"
        return getOrNull(url, serializer)
    }

    /**
     * Retrieves a specific schedule given the event's slug and the schedule's slug.
     *
     * @param event     the slug (or ID) of the event to retrieve the schedule for
     * @param slug      the slug (or ID) of the schedule to retrieve
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the [FullSchedule] if found, null otherwise
     */
    suspend fun getSchedule(event: String, slug: String, hiddenKey: String? = null): FullSchedule? {
        val serializer = ResponseImpl.serializer(FullSchedule.serializer())
        val params = paramToString("hiddenkey", hiddenKey)
        val url = "https://horaro.org/-/api/v1/events/$event/schedules/$slug$params"
        return getOrNull(url, serializer)
    }

    /**
     * Retrieves a specific schedule given the event and the schedule's slug.
     *
     * @param event     the event to retrieve the schedule for
     * @param slug      the slug (or ID) of the schedule to retrieve
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the [FullSchedule] if found, null otherwise
     */
    suspend fun getSchedule(event: Event, slug: String, hiddenKey: String? = null): FullSchedule? {
        return getSchedule(event.id, slug, hiddenKey)
    }

    /**
     * Retrieves the full version of a schedule given a partial one.
     *
     * @param schedule the partial schedule to retrieve the full version for
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the full version of the schedule
     */
    suspend fun getSchedule(schedule: BaseSchedule, hiddenKey: String? = null): FullSchedule {
        return getSchedule(schedule.id, hiddenKey)!!
    }

    /**
     * Retrieves a specific schedule's ticker given the schedule's unique ID.
     *
     * @param id        the ID of the schedule
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the [Ticker] if found, null otherwise
     */
    // IDK for sure if the hiddenKey works here but I *think* it does
    suspend fun getTicker(id: String, hiddenKey: String? = null): Ticker? {
        val serializer = ResponseImpl.serializer(Ticker.serializer())
        val params = paramToString("hiddenkey", hiddenKey)
        val url = "https://horaro.org/-/api/v1/schedules/$id/ticker$params"
        return getOrNull(url, serializer)
    }

    /**
     * Retrieves the specified [schedule]'s ticker.
     *
     * @param schedule  the schedule to retrieve the ticker for
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the [Ticker] if found, null otherwise
     */
    suspend fun getTicker(schedule: BaseSchedule, hiddenKey: String? = null): Ticker? {
        return getTicker(schedule.id, hiddenKey)
    }

    /**
     * Retrieves a specific schedule's ticker given the event's slug and the schedule's slug.
     *
     * @param event     the slug (or ID) of the event to retrieve the schedule for
     * @param slug      the slug (or ID) of the schedule to retrieve
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the [Ticker] if found, null otherwise
     */
    suspend fun getTicker(event: String, slug: String, hiddenKey: String? = null): Ticker? {
        val serializer = ResponseImpl.serializer(Ticker.serializer())
        val params = paramToString("hiddenkey", hiddenKey)
        val url = "https://horaro.org/-/api/v1/events/$event/schedules/$slug/ticker$params"
        return getOrNull(url, serializer)
    }

    /**
     * Retrieves a specific schedule's ticker given the event and the schedule's slug.
     *
     * @param event     the event to retrieve the schedule for
     * @param slug      the slug (or ID) of the schedule to retrieve
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the [Ticker] if found, null otherwise
     */
    suspend fun getTicker(event: Event, slug: String, hiddenKey: String? = null): Ticker? {
        return getTicker(event.id, slug, hiddenKey)
    }
}