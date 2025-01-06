package dev.qixils.gdq

import java.util.regex.Pattern

private val twitchRegex = Pattern.compile("(?:https?://)?(?:www\\.)?twitch\\.tv/(.+)")
private val ytRegex = Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/(.+)")
private val twitterRegex = Pattern.compile("(?:https?://)?(?:www\\.)?twitter\\.com/(.+)")
private val urlRegex = Pattern.compile("https?://.+")

fun computeStreamUrl(stream: String, youtube: String, twitter: String): String? {
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