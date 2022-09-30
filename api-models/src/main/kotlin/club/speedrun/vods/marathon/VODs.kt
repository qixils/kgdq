package club.speedrun.vods.marathon

import dev.qixils.gdq.serializers.DurationAsStringSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.util.regex.Pattern

@Serializable
sealed interface VOD {
    val url: String

    companion object {
        fun fromURL(url: String): VOD {
            return TwitchVOD.fromURL(url)
                ?: YouTubeVOD.fromURL(url)
                ?: GenericVOD(url)
        }
    }
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

    override val url: String = run {
        val sb = StringBuilder("https://www.twitch.tv/videos/").append(videoId)
        if (timestamp != null) sb.append("?t=").append(timestamp)
        sb.toString()
    }

    companion object {
        private const val format = "%dh%dm%ds"
        private val regex = Pattern.compile("^https?://(?:www\\.)?twitch\\.tv/videos/(\\d+)(?:\\?t=(\\w+))?$")

        fun fromURL(url: String): TwitchVOD? {
            val matcher = regex.matcher(url)
            if (!matcher.find()) return null
            val videoId = matcher.group(1)
            val timestamp = matcher.group(2)
            return TwitchVOD(videoId, timestamp)
        }
    }
}

@Serializable
data class YouTubeVOD(
    val videoId: String,
    val timestamp: String?
) : VOD {
    override val url: String = run {
        val sb = StringBuilder("https://youtu.be/").append(videoId)
        if (timestamp != null) sb.append("?t=").append(timestamp)
        sb.toString()
    }

    companion object {
        private val regex = Pattern.compile("^https?://(?:www\\.)?youtu(?:\\.be/|be\\.com/watch\\?v=)([a-zA-Z0-9_-]{11})(?:[?&]t=(\\d+))?$")

        fun fromURL(url: String): YouTubeVOD? {
            val matcher = regex.matcher(url)
            if (!matcher.find()) return null
            val videoId = matcher.group(1)
            val timestamp = matcher.group(2)
            return YouTubeVOD(videoId, timestamp)
        }
    }
}

@Serializable
data class GenericVOD(
    override val url: String
) : VOD
