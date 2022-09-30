package dev.qixils.gdq.models

import kotlinx.serialization.Serializable
import java.util.regex.Pattern

@Serializable
data class Runner(
    val name: String,
    val stream: String,
    val twitter: String,
    val youtube: String,
//    val platform: String?, - this field is always set to "TWITCH" and thus misleading & useless
    val pronouns: String = "",
//    @SerialName("donor") private val _donor: Int? = null,
    val public: String,
) : Model {

    val url: String? = run {
        // Find one of the runner's social media profiles
        if (stream.isNotEmpty()) {
            val twitchMatcher = twitchRegex.matcher(stream)
            val urlMatcher = urlRegex.matcher(stream)
            if (twitchMatcher.matches())
                "https://twitch.tv/${twitchMatcher.group(1)}"
            else if (!urlMatcher.matches())
                "https://twitch.tv/${stream}"
            else
                stream
        } else if (youtube.isNotEmpty()) {
            if (youtube.contains("youtube.com"))
                youtube
            else if (youtube.startsWith("UC", false))
                "https://youtube.com/channel/${youtube}"
            else
                "https://youtube.com/c/${youtube}"
        } else if (twitter.isNotEmpty()) {
            if (twitter.contains("twitter.com"))
                twitter
            else
                "https://twitter.com/${twitter}"
        } else {
            null
        }
    }

    companion object {
        private val twitchRegex = Pattern.compile("https?://(?:www\\.)?twitch\\.tv/(.+)")
        private val urlRegex = Pattern.compile("https?://.+")
    }
}
