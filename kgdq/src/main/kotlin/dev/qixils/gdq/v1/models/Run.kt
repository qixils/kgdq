package dev.qixils.gdq.v1.models

import dev.qixils.gdq.serializers.DurationAsStringSerializer
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Duration
import java.time.Instant

@Deprecated(message = "Use v2 API")
@Serializable
data class Run(
    @SerialName("event") val eventId: Int,
    val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("twitch_name") val twitchName: String = "",
    @SerialName("deprecated_runners") val deprecatedRunners: String,
    val console: String,
    @SerialName("commentators") val rawCommentators: JsonElement,
    @SerialName("hosts") val hostIds: List<Int> = emptyList(),
    val description: String,
    @Serializable(with = InstantAsStringSerializer::class) @SerialName("starttime") private val _startTime: Instant? = null,
    @Serializable(with = InstantAsStringSerializer::class) @SerialName("endtime") private val _endTime: Instant? = null,
    @SerialName("order") private val _order: Int? = null,
    @Serializable(with = DurationAsStringSerializer::class) @SerialName("run_time") val runTime: Duration,
    @Serializable(with = DurationAsStringSerializer::class) @SerialName("setup_time") val setupTime: Duration,
    val coop: Boolean,
    val category: String = "Any%",
    @SerialName("release_year") val releaseYear: Int?,
    @SerialName("runners") private val runnerIds: List<Int>,
    @SerialName("canonical_url") private var _canonicalUrl: String? = null,
    val public: String,
    @SerialName("external_id") val horaroId: String? = null, // ESA exclusive
) : AbstractModel() {

    override fun isValid(): Boolean {
        return _startTime != null && _endTime != null && _order != null
    }

    val startTime: Instant get() = _startTime!!
    val endTime: Instant get() = _endTime!!
    val order: Int get() = _order!!

    val runTimeText: String get() = DurationAsStringSerializer.format(runTime)
    val setupTimeText: String get() = DurationAsStringSerializer.format(setupTime)

    val canonicalUrl: String get() = _canonicalUrl
        ?: (api.apiPath.replaceFirst("/search/", "/run/", false) + id)

    suspend fun fetchEvent(): Event {
        return api.getEvent(eventId)!!
    }

    suspend fun fetchRunners(): List<Runner> {
        return runnerIds.mapNotNull { api.getRunner(it) }
    }

    suspend fun fetchCommentators(): List<Headset> {
        return when (rawCommentators) {
            is JsonPrimitive ->
                rawCommentators.contentOrNull?.split(", ?".toRegex())?.map(String::trim)?.filter(String::isNotEmpty)?.map { Headset(it) } ?: emptyList()
            is JsonArray ->
                rawCommentators.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull()?.let { id -> api.getHeadset(id) } }
            else ->
                throw IllegalStateException("Unknown commentators type: ${rawCommentators::class.simpleName}")
        }
    }

    suspend fun fetchHosts(): List<Headset> {
        return hostIds.mapNotNull { api.getHeadset(it) }
    }
}
