package club.speedrun.vods.marathon

import kotlinx.serialization.Serializable

@Serializable
sealed interface VOD {
    fun asURL(): String
}

@Serializable
data class TwitchVOD(
    val videoId: Int,
    val timestamp: String?
) : VOD {
    override fun asURL(): String {
        val sb = StringBuilder("https://www.twitch.tv/videos/").append(videoId)
        if (timestamp != null) sb.append("?t=").append(timestamp)
        return sb.toString()
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
