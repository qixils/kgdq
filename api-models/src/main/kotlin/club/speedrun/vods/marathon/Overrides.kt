package club.speedrun.vods.marathon

import club.speedrun.vods.db.Identified
import club.speedrun.vods.db.ULID
import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Run
import dev.qixils.gdq.models.Wrapper
import dev.qixils.gdq.serializers.DurationAsSecondsSerializer
import dev.qixils.gdq.serializers.InstantAsSecondsSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

@Serializable
data class RunOverrides(
    override val id: String = ULID.random(),
    var runId: Int? = null,
    var horaroId: String? = null,
    val vods: MutableList<VOD> = mutableListOf(),
    val vodSuggestions: MutableList<VodSuggestion> = mutableListOf(),
    @Serializable(with = InstantAsSecondsSerializer::class) var startTime: Instant? = null,
    @Serializable(with = DurationAsSecondsSerializer::class) var runTime: Duration? = null,
    var src: String? = null,
) : Identified {
    constructor(run: Wrapper<Run>) : this(
        runId = run.id,
        horaroId = run.value.horaroId
    )

    constructor(run: dev.qixils.horaro.models.Run) : this(
        horaroId = run.getValue("ID")
    )

    fun mergeIn(other: RunOverrides) {
        if (runId == null) runId = other.runId
        if (horaroId == null) horaroId = other.horaroId
        if (startTime == null) startTime = other.startTime
        if (runTime == null) runTime = other.runTime
        if (src == null) src = other.src
        vods.addAll(other.vods)
        vodSuggestions.addAll(other.vodSuggestions)
    }

    companion object {
        const val COLLECTION_NAME = "RunOverrides"
    }
}

@Serializable
data class EventOverrides(
    override val id: String,
    @Serializable(with = InstantAsSecondsSerializer::class) var startedAt: Instant? = null,
    @Serializable(with = InstantAsSecondsSerializer::class) var endedAt: Instant? = null, // TODO: cache this less readily
    var redditMergedIn: Boolean = false,
) : Identified {
    constructor(event: Wrapper<Event>) : this(
        id = event.id.toString(),
    )

    companion object {
        const val COLLECTION_NAME = "EventOverrides"
    }
}