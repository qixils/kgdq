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
    override val id: String = ULID.random(),
    var runId: Int? = null,
    var horaroId: String? = null,
    val twitchVODs: MutableList<TwitchVOD> = mutableListOf(),
    val youtubeVODs: MutableList<YouTubeVOD> = mutableListOf(),
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
        twitchVODs.addAll(other.twitchVODs)
        youtubeVODs.addAll(other.youtubeVODs)
    }

    companion object {
        const val COLLECTION_NAME = "RunOverrides"
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