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

// TODO: this class is i guess silently failing to serialize.
//  - probably because of the VOD objects?
//  - or maybe Duration?
//  - definitely shouldn't be the Instant tho because it works for EventOverrides.
//  - either way, I can't think of a good solution besides maybe storing everything as strings or to
//  swap out the DB serializers for something else.
//  - i should also try running registerSerializer() with my DurationAsStringSerializer and see if
//  that fixes it.
//  - hey, now that i think about it, why does updating fail but inserting works???
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
    constructor(runId: Int?, horaroId: String?) : this(
        _id = newId(),
        runId = runId,
        horaroId = horaroId,
    )

    constructor(run: Wrapper<Run>) : this(
        _id = newId(),
        runId = run.id,
        horaroId = run.value.horaroId
    )

    constructor(run: dev.qixils.horaro.models.Run) : this(
        _id = newId(),
        horaroId = run.getValue("ID")
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