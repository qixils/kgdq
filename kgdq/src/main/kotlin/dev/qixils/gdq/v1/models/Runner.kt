package dev.qixils.gdq.v1.models

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
) : AbstractModel() {

    val url: String? = computeUrl()

    private fun computeUrl(): String? {
        // Find one of the runner's social media profiles
        return if (stream.isNotEmpty()) {
            val twitchMatcher = twitchRegex.matcher(stream)
            val urlMatcher = urlRegex.matcher(stream)
            if (twitchMatcher.matches())
                "https://twitch.tv/${twitchMatcher.group(1)}"
            else if (!urlMatcher.matches())
                "https://twitch.tv/${stream}"
            else
                stream
        } else if (youtube.isNotEmpty()) {
            val ytMatcher = ytRegex.matcher(youtube)
            if (ytMatcher.matches())
                "https://youtube.com/${ytMatcher.group(1)}"
            else if (youtube.startsWith("UC", false))
                "https://youtube.com/channel/${youtube}"
            else
                "https://youtube.com/@${youtube}"
        } else if (twitter.isNotEmpty()) {
            val twitterMatcher = twitterRegex.matcher(twitter)
            if (twitterMatcher.matches())
                "https://twitter.com/${twitterMatcher.group(1)}"
            else
                "https://twitter.com/${twitter}"
        } else {
            null
        }
    }

    companion object {
        private val twitchRegex = Pattern.compile("(?:https?://)?(?:www\\.)?twitch\\.tv/(.+)")
        private val ytRegex = Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/(.+)")
        private val twitterRegex = Pattern.compile("(?:https?://)?(?:www\\.)?twitter\\.com/(.+)")
        private val urlRegex = Pattern.compile("https?://.+")
    }
}
