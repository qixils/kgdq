package club.speedrun.vods.marathon

import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Run
import dev.qixils.gdq.models.Wrapper
import dev.qixils.gdq.serializers.DurationAsStringSerializer
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.time.Duration
import java.time.Instant

@Serializable
data class RunOverrides(
    @Contextual val _id: Id<RunOverrides>,
    var runId: Int? = null,
    var horaroId: String? = null,
    val twitchVODs: MutableList<TwitchVOD> = mutableListOf(),
    val youtubeVODs: MutableList<YouTubeVOD> = mutableListOf(),
    @Serializable(with = InstantAsStringSerializer::class) var startTime: Instant? = null,
    @Serializable(with = DurationAsStringSerializer::class) var runTime: Duration? = null,
) {
    constructor(run: Wrapper<Run>) : this(
        _id = newId<RunOverrides>(),
        runId = run.id,
        horaroId = run.value.horaroId
    )

    constructor(run: dev.qixils.horaro.models.Run) : this(
        _id = newId<RunOverrides>(),
        horaroId = run.getValue("ID", true)
    )

    fun mergeIn(other: RunOverrides) {
        if (runId == null) runId = other.runId
        if (horaroId == null) horaroId = other.horaroId
        if (startTime == null) startTime = other.startTime
        if (runTime == null) runTime = other.runTime
        twitchVODs.addAll(other.twitchVODs)
        youtubeVODs.addAll(other.youtubeVODs)
    }

    companion object {
        const val COLLECTION_NAME = "RunOverrides"
    }
}

@Serializable
data class EventOverrides(
    val _id: String,
    @Serializable(with = InstantAsStringSerializer::class) var datetime: Instant? = null,
) {
    constructor(event: Event) : this(
        _id = event.short,
    )

    companion object {
        const val COLLECTION_NAME = "EventOverrides"
    }
}