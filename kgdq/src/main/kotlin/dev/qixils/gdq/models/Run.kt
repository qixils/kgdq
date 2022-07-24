package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.serializers.DurationSerializer
import dev.qixils.gdq.serializers.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration
import java.time.Instant

@Serializable
data class Run(
    @SerialName("event") private val eventId: Int,
    val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("twitch_name") val twitchName: String,
    @SerialName("deprecated_runners") val deprecatedRunners: String,
    val console: String,
    val commentators: String,
    val description: String,
    @Serializable(with = InstantSerializer::class) @SerialName("starttime") val startTime: Instant,
    @Serializable(with = InstantSerializer::class) @SerialName("endtime") val endTime: Instant,
    val order: Int,
    @Serializable(with = DurationSerializer::class) @SerialName("run_time") val runTime: Duration,
    @Serializable(with = DurationSerializer::class) @SerialName("setup_time") val setupTime: Duration,
    val coop: Boolean,
    val category: String = "Any%",
    @SerialName("release_year") val releaseYear: Int?,
    @SerialName("runners") private val runnerIds: List<Int>,
    @SerialName("canonical_url") val canonicalUrl: String,
    val public: String
) : Model {

    val runTimeText: String get() = DurationSerializer.format(runTime)
    val setupTimeText: String get() = DurationSerializer.format(setupTime)

    @Transient private var _event: Wrapper<Event>? = null
    @Transient private var _runners: List<Wrapper<Runner>>? = null

    override suspend fun loadData(api: GDQ) {
        _event = api.query(type=ModelType.EVENT, id=eventId).first()
        _runners = runnerIds.map { api.query(type=ModelType.RUNNER, id=it).first() }
    }

    val event: Wrapper<Event> get() = _event!!
    val runners: List<Wrapper<Runner>> get() = _runners!!
}
