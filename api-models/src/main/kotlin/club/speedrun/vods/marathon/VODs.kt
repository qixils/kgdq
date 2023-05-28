package club.speedrun.vods.marathon

import club.speedrun.vods.db.ULID
import dev.qixils.gdq.serializers.DurationAsStringSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.util.regex.Pattern

@Serializable
data class VOD(
    val type: VODType,
    val videoId: String? = null,
    val timestamp: String? = null,
    val url: String,
    val contributorId: String? = null, // TODO: expose user's name in API
) {
    companion object {
        fun fromUrlOrNull(url: String, contributor: String? = null): VOD? {
            return VODType.TWITCH.fromUrl(url, contributor) ?: VODType.YOUTUBE.fromUrl(url, contributor)
        }
        fun fromUrl(url: String, contributor: String? = null): VOD {
            return fromUrlOrNull(url, contributor) ?: VODType.OTHER.fromUrl(url, contributor)!!
        }
        fun fromUrlOrThrow(url: String, contributor: String? = null): VOD {
            return fromUrlOrNull(url, contributor) ?: throw IllegalArgumentException("Invalid VOD URL: $url")
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
        override fun fromUrl(url: String, contributor: String?): VOD? {
            val matcher = urlRegex.matcher(url)
            if (!matcher.find()) return null
            val videoId = matcher.group(1)
            val timestamp = matcher.group(2)
            return fromParts(videoId, timestamp, contributor)
        }
        override fun fromParts(videoId: String, timestamp: String?, contributor: String?): VOD {
            if (!videoIdRegex.matcher(videoId).matches())
                throw IllegalArgumentException("Invalid Twitch video ID: $videoId")
            val sb = StringBuilder("https://www.twitch.tv/videos/").append(videoId)
            if (timestamp != null) {
                if (!timestampRegex.matcher(timestamp).matches())
                    throw IllegalArgumentException("Invalid Twitch timestamp: $timestamp")
                sb.append("?t=").append(timestamp)
            }
            return VOD(this, videoId, timestamp, sb.toString(), contributor)
        }
        override fun fromParts(videoId: String, timestamp: Duration?, contributor: String?): VOD {
            return fromParts(videoId, timestamp?.let { DurationAsStringSerializer.format(it, format) }, contributor)
        }
    },
    YOUTUBE {
        private val urlRegex = Pattern.compile("^https?://(?:www\\.)?youtu(?:\\.be/|be\\.com/watch\\?v=)([a-zA-Z0-9_-]{11})(?:[?&]t=(\\d+))?$")
        private val videoIdRegex = Pattern.compile("^[a-zA-Z0-9_-]{11}$")
        private val timestampRegex = Pattern.compile("^\\d+$")
        override fun fromUrl(url: String, contributor: String?): VOD? {
            val matcher = urlRegex.matcher(url)
            if (!matcher.find()) return null
            val videoId = matcher.group(1)
            val timestamp = matcher.group(2)
            return fromParts(videoId, timestamp, contributor)
        }
        override fun fromParts(videoId: String, timestamp: String?, contributor: String?): VOD {
            if (!videoIdRegex.matcher(videoId).matches())
                throw IllegalArgumentException("Invalid YouTube video ID: $videoId")
            val sb = StringBuilder("https://youtu.be/").append(videoId)
            if (timestamp != null) {
                if (!timestampRegex.matcher(timestamp).matches())
                    throw IllegalArgumentException("Invalid YouTube timestamp: $timestamp")
                sb.append("?t=").append(timestamp)
            }
            return VOD(this, videoId, timestamp, sb.toString(), contributor)
        }
        override fun fromParts(videoId: String, timestamp: Duration?, contributor: String?): VOD {
            return fromParts(videoId, timestamp?.seconds?.toString(), contributor)
        }
    },
    OTHER {
        override fun fromUrl(url: String, contributor: String?): VOD {
            return VOD(this, url = url, contributorId = contributor)
        }
        override fun fromParts(videoId: String, timestamp: String?, contributor: String?): VOD {
            throw UnsupportedOperationException("Cannot create OTHER VOD from parts")
        }
        override fun fromParts(videoId: String, timestamp: Duration?, contributor: String?): VOD {
            throw UnsupportedOperationException("Cannot create OTHER VOD from parts")
        }
    };
    abstract fun fromUrl(url: String, contributor: String? = null): VOD?
    abstract fun fromParts(videoId: String, timestamp: String?, contributor: String? = null): VOD
    abstract fun fromParts(videoId: String, timestamp: Duration?, contributor: String? = null): VOD
}

@Serializable
data class VodSuggestion(
    val vod: VOD,
    val id: String = ULID.random(),
    var state: VodSuggestionState = VodSuggestionState.PENDING,
)

@Serializable
enum class VodSuggestionState {
    PENDING,
    APPROVED,
    REJECTED,
}
