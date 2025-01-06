package club.speedrun.vods.marathon

import club.speedrun.vods.db.Identified
import dev.qixils.gdq.serializers.DurationAsSecondsSerializer
import dev.qixils.gdq.serializers.InstantAsSecondsSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

@Serializable
data class RunOverrides(
    override val id: String,
    val vods: MutableList<VOD> = mutableListOf(),
    @Deprecated(message = "Suggestions are now saved separately")
    val vodSuggestions: MutableList<VodSuggestion> = mutableListOf(),
    @Serializable(with = InstantAsSecondsSerializer::class) var startTime: Instant? = null,
    @Serializable(with = DurationAsSecondsSerializer::class) var runTime: Duration? = null,
    var src: String? = null,
) : Identified {
    companion object {
        const val COLLECTION_NAME = "RunOverrides"
    }
}

@Serializable
data class EventOverrides(
    override val id: String,
    @Serializable(with = InstantAsSecondsSerializer::class) var startedAt: Instant? = null,
    @Serializable(with = InstantAsSecondsSerializer::class) var endedAt: Instant? = null,
    var redditMergedIn: Boolean = false,
) : Identified {
    companion object {
        const val COLLECTION_NAME = "EventOverrides"
    }
}