package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.serializers.DurationAsStringSerializer
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration
import java.time.Instant

@Serializable
data class Run(
    @SerialName("event") val eventId: Int,
    val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("twitch_name") val twitchName: String = "",
    @SerialName("deprecated_runners") val deprecatedRunners: String,
    val console: String,
    val commentators: String,
    val description: String,
    @Serializable(with = InstantAsStringSerializer::class) @SerialName("starttime") val _startTime: Instant? = null,
    @Serializable(with = InstantAsStringSerializer::class) @SerialName("endtime") val _endTime: Instant? = null,
    @SerialName("order") private val _order: Int? = null,
    @Serializable(with = DurationAsStringSerializer::class) @SerialName("run_time") val runTime: Duration,
    @Serializable(with = DurationAsStringSerializer::class) @SerialName("setup_time") val setupTime: Duration,
    val coop: Boolean,
    val category: String = "Any%",
    @SerialName("release_year") val releaseYear: Int?,
    @SerialName("runners") private val runnerIds: List<Int>,
    @SerialName("canonical_url") private var _canonicalUrl: String? = null,
    val public: String
) : Model {

    override fun isValid(): Boolean {
        return _startTime != null && _endTime != null && _order != null
    }

    val startTime: Instant get() = _startTime!!
    val endTime: Instant get() = _endTime!!
    val order: Int get() = _order!!

    val runTimeText: String get() = DurationAsStringSerializer.format(runTime)
    val setupTimeText: String get() = DurationAsStringSerializer.format(setupTime)

    @Transient private var api: GDQ? = null
    @Transient private var id: Int? = null
    @Transient private var _event: Wrapper<Event>? = null
    @Transient private var _runners: List<Wrapper<Runner>>? = null

    override suspend fun loadData(api: GDQ, id: Int) {
        this.api = api
        this.id = id
        // canonical URL fallback
        if (_canonicalUrl == null)
            _canonicalUrl = api.apiPath.replaceFirst("/search/", "/run/", false) + id
    }

    val canonicalUrl: String get() = _canonicalUrl!!

    suspend fun event(): Wrapper<Event> {
        if (_event == null)
            _event = api!!.query(type = ModelType.EVENT, id = eventId).first()
        return _event!!
    }

    suspend fun runners(): List<Wrapper<Runner>> {
        if (_runners == null) {
            val allRunners = api!!.query(type = ModelType.RUNNER, run = id!!, event = eventId)
            _runners = runnerIds.map { runnerId -> allRunners.first { it.id == runnerId } }
        }
        return _runners!!
    }
}
