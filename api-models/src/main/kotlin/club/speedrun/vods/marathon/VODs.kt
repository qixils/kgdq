package club.speedrun.vods.marathon

import dev.qixils.gdq.serializers.DurationAsStringSerializer
import kotlinx.serialization.Serializable
import java.time.Duration

@Serializable
sealed interface VOD {
    fun asURL(): String
}

@Serializable
data class TwitchVOD(
    val videoId: String,
    val timestamp: String? = null
) : VOD {
    constructor(videoId: String, duration: Duration) : this(
        videoId,
        DurationAsStringSerializer.format(duration, format)
    )

    override fun asURL(): String {
        val sb = StringBuilder("https://www.twitch.tv/videos/").append(videoId)
        if (timestamp != null) sb.append("?t=").append(timestamp)
        return sb.toString()
    }

    companion object {
        private const val format = "%dh%dm%ds"
    }
}

@Serializable
data class YouTubeVOD(
    val videoId: String,
    val timestamp: String?
) : VOD {
    override fun asURL(): String {
        val sb = StringBuilder("https://youtu.be/").append(videoId)
        if (timestamp != null) sb.append("?t=").append(timestamp)
        return sb.toString()
    }
}
