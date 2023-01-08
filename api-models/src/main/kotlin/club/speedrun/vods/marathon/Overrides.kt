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
    val vods: MutableList<VOD>,
    val vodSuggestions: MutableList<VodSuggestion>,
    @Serializable(with = InstantAsSecondsSerializer::class) var startTime: Instant? = null,
    @Serializable(with = DurationAsSecondsSerializer::class) var runTime: Duration? = null,
    var src: String? = null,
) : Identified {
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
        fun create(runId: Int? = null, horaroId: String? = null) = RunOverrides(
            runId = runId,
            horaroId = horaroId,
            vods = mutableListOf(),
            vodSuggestions = mutableListOf()
        )
        fun create(run: Wrapper<Run>) = RunOverrides(
            runId = run.id,
            horaroId = run.value.horaroId,
            vods = mutableListOf(),
            vodSuggestions = mutableListOf()
        )
        fun create(run: dev.qixils.horaro.models.Run) = RunOverrides(
            horaroId = run.getValue("ID"),
            vods = mutableListOf(),
            vodSuggestions = mutableListOf()
        )
    }
}

@Serializable
data class EventOverrides(
    override val id: String,
    @Serializable(with = InstantAsSecondsSerializer::class) var datetime: Instant? = null,
) : Identified {
    constructor(event: Event) : this(
        id = event.short,
    )

    companion object {
        const val COLLECTION_NAME = "EventOverrides"
    }
}