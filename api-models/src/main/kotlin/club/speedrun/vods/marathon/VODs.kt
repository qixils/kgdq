package club.speedrun.vods.marathon

import dev.qixils.gdq.serializers.DurationAsStringSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.util.regex.Pattern

@Serializable
data class VOD(
    val type: VODType,
    val videoId: String? = null,
    val timestamp: String? = null,
    val url: String
) {
    companion object {
        fun fromUrlOrNull(url: String): VOD? {
            return VODType.TWITCH.fromUrl(url) ?: VODType.YOUTUBE.fromUrl(url)
        }
        fun fromUrl(url: String): VOD {
            return fromUrlOrNull(url) ?: VODType.OTHER.fromUrl(url)!!
        }
        fun fromUrlOrThrow(url: String): VOD {
            return fromUrlOrNull(url) ?: throw IllegalArgumentException("Invalid VOD URL: $url")
        }
    }
}

@Serializable
enum class VODType {
    TWITCH {
        private val format = "%dh%dm%ds"
        private val urlRegex = Pattern.compile("^https?://(?:www\\.)?twitch\\.tv/videos/(\\d+)(?:\\?t=(\\w+))?$")
        private val videoIdRegex = Pattern.compile("^\\d+$")
        private val timestampRegex = Pattern.compile("^\\d{1,2}h\\d{1,2}m\\d{1,2}s$")
        override fun fromUrl(url: String): VOD? {
            val matcher = urlRegex.matcher(url)
            if (!matcher.find()) return null
            val videoId = matcher.group(1)
            val timestamp = matcher.group(2)
            return fromParts(videoId, timestamp)
        }
        override fun fromParts(videoId: String, timestamp: String?): VOD {
            if (!videoIdRegex.matcher(videoId).matches())
                throw IllegalArgumentException("Invalid Twitch video ID: $videoId")
            val sb = StringBuilder("https://www.twitch.tv/videos/").append(videoId)
            if (timestamp != null) {
                if (!timestampRegex.matcher(timestamp).matches())
                    throw IllegalArgumentException("Invalid Twitch timestamp: $timestamp")
                sb.append("?t=").append(timestamp)
            }
            return VOD(this, videoId, timestamp, sb.toString())
        }
        override fun fromParts(videoId: String, timestamp: Duration?): VOD {
            return fromParts(videoId, timestamp?.let { DurationAsStringSerializer.format(it, format) })
        }
    },
    YOUTUBE {
        private val urlRegex = Pattern.compile("^https?://(?:www\\.)?youtu(?:\\.be/|be\\.com/watch\\?v=)([a-zA-Z0-9_-]{11})(?:[?&]t=(\\d+))?$")
        private val videoIdRegex = Pattern.compile("^[a-zA-Z0-9_-]{11}$")
        private val timestampRegex = Pattern.compile("^\\d+$")
        override fun fromUrl(url: String): VOD? {
            val matcher = urlRegex.matcher(url)
            if (!matcher.find()) return null
            val videoId = matcher.group(1)
            val timestamp = matcher.group(2)
            return fromParts(videoId, timestamp)
        }
        override fun fromParts(videoId: String, timestamp: String?): VOD {
            if (!videoIdRegex.matcher(videoId).matches())
                throw IllegalArgumentException("Invalid YouTube video ID: $videoId")
            val sb = StringBuilder("https://youtu.be/").append(videoId)
            if (timestamp != null) {
                if (!timestampRegex.matcher(timestamp).matches())
                    throw IllegalArgumentException("Invalid YouTube timestamp: $timestamp")
                sb.append("?t=").append(timestamp)
            }
            return VOD(this, videoId, timestamp, sb.toString())
        }
        override fun fromParts(videoId: String, timestamp: Duration?): VOD {
            return fromParts(videoId, timestamp?.seconds?.toString())
        }
    },
    OTHER {
        override fun fromUrl(url: String): VOD {
            return VOD(this, url = url)
        }
        override fun fromParts(videoId: String, timestamp: String?): VOD {
            throw UnsupportedOperationException("Cannot create OTHER VOD from parts")
        }
        override fun fromParts(videoId: String, timestamp: Duration?): VOD {
            throw UnsupportedOperationException("Cannot create OTHER VOD from parts")
        }
    };
    abstract fun fromUrl(url: String): VOD?
    abstract fun fromParts(videoId: String, timestamp: String?): VOD
    abstract fun fromParts(videoId: String, timestamp: Duration?): VOD
}

@Serializable
data class VodSuggestion(
    val vod: VOD,
    val submitter: String, // ID
)