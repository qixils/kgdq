package dev.qixils.gdq.reddit

import club.speedrun.vods.client.SvcClient
import club.speedrun.vods.marathon.EventData
import club.speedrun.vods.marathon.RunData
import club.speedrun.vods.marathon.VOD
import club.speedrun.vods.marathon.VODType
import club.speedrun.vods.naturalJoinTo
import dev.qixils.gdq.models.Runner
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.dean.jraw.RateLimitException
import net.dean.jraw.RedditClient
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class ThreadManager(
    private val reddit: RedditClient,
    private val config: ThreadConfig,
) {
    private val logger = LoggerFactory.getLogger(ThreadManager::class.java)
    private val client = SvcClient()
    private val marathonClient = client.getMarathonClient(config.org.lowercase(Locale.ENGLISH))
    private val marathon = runBlocking { marathonClient.get(stats = false) }

    init {
        logger.debug("Loaded thread manager for ${config.org}")
    }

    companion object {
        private var lastUpdate = Instant.MIN
    }

    private suspend fun generateBody(): StringBuilder {
        val body = StringBuilder()
        generateMainHeader(body)
        for (event in config.events) {
            generateSubBody(body, event)
        }
        return body
    }

    private suspend fun generateSubBody(body: StringBuilder, eventConfig: EventConfig): CharSequence {
        // Get event and run data
        val event = marathonClient.getEvent(eventConfig.eventId) ?: throw IllegalStateException("Event ${eventConfig.eventId} by ${config.org} not found")
        val runs = marathonClient.getRuns(eventConfig.eventId)

        // Generate body
        generateSubHeader(body, event, eventConfig)
        for (run in runs) {
            generateRunRow(body, run)
        }
        return body
    }

    private fun generateMainHeader(body: StringBuilder) {
        body.append("The event's schedule may change without notice so this thread can be out-of-date by several minutes. ")
            .append("Please check the event's website for the most accurate reference.\n\n")
            .append("Don't gild the thread, donate the money instead! \\^_\\^\n\n")
            .append("This thread is powered by the [thread updater](https://github.com/qixils/kgdq/tree/main/reddit) ")
            .append("using data from [${marathon.displayName}](${marathon.homepageUrl})")
        if (!marathon.autoVODs)
            body.append(", [Speedrun.com](https://www.speedrun.com), and the contributors to the ")
                .append("[VOD site](https://vods.speedrun.club/). ")
                .append("Thank you to the volunteers that keep this thread running!\n")
        else
            body.append(" and [Speedrun.com](https://www.speedrun.com).\n")
    }

    private fun generateSubHeader(body: StringBuilder, event: EventData, eventConfig: EventConfig) {
        body.append("\n")
        if (config.events.size > 1)
            body.append("# ${eventConfig.displayName}\n\n")
        body.append("## Links\n\n")
            .append("* **New:** [Watch and submit VODs on the VOD site](https://vods.speedrun.club/event/${config.org}/${event.short})\n")
            .append("* [Watch ${eventConfig.displayName}](https://twitch.tv/${eventConfig.twitch})\n")
            .append("* [Donate to ${event.charityName}](${event.donationUrl})\n")
            .append("* [Official Schedule](${event.scheduleUrl})\n")
            .append("* [${marathon.shortName} YouTube playlist](")
        if (eventConfig.playlist != null)
            body.append("https://www.youtube.com/playlist?list=").append(eventConfig.playlist)
        else
            body.append("https://www.youtube.com/@").append(config.youtube)
        body.append(")\n")
        body.append("\n## Schedule\n\n")
            .append("Game | Runner / Channel | Time / Link\n")
            .append("--|--|:--:|\n")
    }

    private fun generateRunRow(body: StringBuilder, run: RunData) {
        //// game name, category, SR.com link
        body.append(run.name)
        if (run.category.isNotEmpty())
            body.append(" (").append(run.category).append(")")
        if (run.src != null)
            body.append("^[+](https://www.speedrun.com/").append(run.src).append(")")
        body.append(" | ")

        //// runners
        naturalJoinTo(body, run.runners) { generateRunner(it) }
        body.append(" | ")

        //// time, VODs
        body.append(generateTime(run))

        //// end row
        body.appendLine()
    }

    private fun generateRunner(runner: Runner): String {
        // Return markdown-formatted name with link if available
        return runner.url?.let { "[${runner.name}]($it)" } ?: runner.name
    }

    private fun appendMiscVODs(sb: StringBuilder, vods: List<VOD>, id: String) {
        vods.forEachIndexed { index, vod ->
            sb.append(" [").append(id)
            if (index > 0) sb.append(index + 1)
            sb.append("](").append(vod.url).append(")")
        }
    }

    private fun generateTime(run: RunData): CharSequence {
        val time = StringBuilder()
        val twitchVODs = run.vods.filter { it.type == VODType.TWITCH }
        val ytVODs = run.vods.filter { it.type == VODType.YOUTUBE }
        val otherVODs = run.vods.filter { it.type == VODType.OTHER }
        // add twitch VODs if available
        if (twitchVODs.isNotEmpty()) {
            twitchVODs.forEachIndexed { index, vod ->
                if (index == 0)
                    time.append('[').append(run.runTimeText)
                else
                    time.append(" [\\[").append(index + 1).append("\\]")
                time.append("](").append(vod.url).append(')')
            }
        } else {
            // otherwise, add run time with no link
            time.append(run.runTimeText)
        }
        // add YouTube VODs if available
        appendMiscVODs(time, ytVODs, "YT")
        appendMiscVODs(time, otherVODs, "MISC")
        // add italics if the run is ongoing
        if (run.isCurrent)
            time.insert(0, '*').append('*')
        return time
    }

    suspend fun run() {
        logger.info("Running thread manager for ${config.org}")

        // Update thread
        try {
            // Generate body
            val body = generateBody().toString()
            // Wait until 5 seconds have elapsed since the last update to avoid rate limiting
            while (lastUpdate.isAfter(Instant.now().minusSeconds(5)))
                delay(1000)
            // Perform update
            while (true) {
                try {
                    reddit.submission(config.threadId).edit(body)
                    break
                } catch (e: RateLimitException) {
                    logger.info("Got ratelimited, trying again in ${e.cooldown.toInt()} seconds")
                    delay(e.cooldown.seconds)
                }
            }
            lastUpdate = Instant.now()
            logger.info("Updated thread ${config.threadId} for ${config.org}")
        } catch (e: Exception) {
            logger.error("Failed to update thread ${config.threadId} for ${config.org}", e)
        }
    }
}