package club.speedrun.vods

import club.speedrun.vods.marathon.VOD
import club.speedrun.vods.marathon.VODType
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.util.regex.Pattern

object RedditUtils {
    suspend inline fun <reified T> getRedditWiki(id: String, logErrors: Boolean = true): List<T> {
        val response = httpClient.get("https://www.reddit.com/r/VODThread/wiki/$id.json") {
            header(HttpHeaders.UserAgent, "GDQ VODs API v2 (/u/noellekiq)")
        }
        if (response.status != HttpStatusCode.OK) {
            if (logErrors) logger.warn("Failed to fetch wiki for $id: ${response.status}")
            return emptyList()
        }
        // strip leading spaces and trailing comments
        val body: JsonObject = response.body()
        val content = body["data"]?.jsonObject?.get("content_md")?.jsonPrimitive?.content
            ?.split(Pattern.compile("\\r?\\n"))
            ?.joinToString("") { it.split('#', limit=2)[0].trim() }
            ?: return emptyList()
        return json.decodeFromString(content)
    }

    suspend inline fun getRedditTwitchVODs(short: String, logErrors: Boolean = true): List<List<VOD>> {
        return getRedditWiki<List<String>>("${short}vods", logErrors).map {
            val strs = it.toMutableList()
            val vods = mutableListOf<VOD>()
            while (strs.size >= 2) {
                val videoId = strs.removeFirst()
                val timestamp = strs.removeFirst()
                try {
                    vods.add(VODType.TWITCH.fromParts(videoId, timestamp))
                } catch (e: Exception) {
                    if (logErrors) logger.warn("Failed to parse Twitch VOD $videoId ($timestamp) for $short", e)
                }
            }
            if (strs.isNotEmpty() && logErrors)
                logger.warn("Excess data found for Twitch VODs for $short: ${strs.first()}")
            vods
        }
    }

    suspend inline fun getRedditYouTubeVODs(short: String): List<List<VOD>> {
        return getRedditWiki<JsonElement>("${short}yt").map { element ->
            val strs = if (element is JsonArray)
                element.map { it.jsonPrimitive.content }
            else
                listOf(element.jsonPrimitive.content)
            strs.mapNotNull {
                try {
                    VODType.YOUTUBE.fromUrl("https://youtu.be/$it")
                } catch (e: Exception) {
                    logger.warn("Failed to parse YouTube VOD $it for $short", e)
                    null
                }
            }
        }
    }

    suspend inline fun getRedditVODs(short: String): List<List<VOD>> {
        val twitch = try { getRedditTwitchVODs(short) } catch (e: Exception) { logger.error("Failed to load Twitch VODs for $short", e); emptyList() }
        val yt = try { getRedditYouTubeVODs(short) } catch (e: Exception) { logger.error("Failed to load YouTube VODs for $short", e); emptyList() }
        // merge
        val vods = mutableListOf<List<VOD>>()
        for (i in 0 until maxOf(twitch.size, yt.size)) {
            val list = mutableListOf<VOD>()
            if (i < twitch.size) list.addAll(twitch[i])
            if (i < yt.size) list.addAll(yt[i])
            vods.add(list)
        }
        return vods
    }
}