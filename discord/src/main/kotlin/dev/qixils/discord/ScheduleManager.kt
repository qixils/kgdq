package dev.qixils.discord

import club.speedrun.vods.marathon.EventData
import club.speedrun.vods.marathon.RunData
import club.speedrun.vods.naturalJoinTo
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.qixils.gdq.models.BidState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.utils.TimeFormat
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ScheduleManager(
    private val bot: Bot,
    private val config: EventConfig,
) {
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build()
    private val apiRoot = "https://vods.speedrun.club/api/v1/${config.organization.name.lowercase(Locale.ENGLISH)}/"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScheduleManager::class.java)
        private val dateHeaderFormat = DateTimeFormatter.ofPattern("_ _\n> **EEEE** MMM d\n_ _\n", Locale.ENGLISH)
        private val twitchRegex = Pattern.compile("https?://(?:www\\.)?twitch\\.tv/(.+)")
        private val urlRegex = Pattern.compile("https?://.+")
    }

    init {
        logger.debug("Starting schedule manager for ${config.organization.name}'s ${config.id}...")
        scheduler.scheduleAtFixedRate(this::run, 0, config.waitMinutes, TimeUnit.MINUTES)
    }

    private fun <M> get(query: String, serializer: KSerializer<M>): M {
        val uri = URI(apiRoot + query)
        logger.debug("GET $uri")
        val request = HttpRequest.newBuilder(uri).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return json.decodeFromString(serializer, response.body())
    }

    // TODO split out a lot of this stuff into functions
    private fun run() = runBlocking {
        logger.info("Started schedule manager for ${config.organization.name}'s ${config.id}")

        // Get event and run data
        val event = get("events?id=${config.id}", ListSerializer(EventData.serializer()))
            .firstOrNull() ?: throw IllegalStateException("Event ${config.id} by ${config.organization.name} not found")
        val runs = get("runs?event=${config.id}", ListSerializer(RunData.serializer()))

        // Initialize misc utility vals
        val moneyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        moneyFormatter.currency = Currency.getInstance(event.paypalCurrency)

        // Create list of messages
        val messages = mutableListOf<MessageTransformer>()
        val runTicker = mutableListOf<RunData>()

        // Add header message
        messages.add(
            MessageTransformer(
                "**${event.name}**\n" +
                        "Date headers are in the ${event.timezone.id} timezone.\n" +
                        "Join the ${event.count} donators who have raised " +
                        "${moneyFormatter.format(event.amount)} for ${event.charityName} " +
                        "at ${event.canonicalUrl}. " +
                        "(Minimum Donation: ${moneyFormatter.format(event.minimumDonation)})",
                pin = true
            )
        )

        // Add message for each run
        runs.forEachIndexed { index, run ->
            val sb = StringBuilder()
            val tz = event.timezone
            if (index == 0 || runs[index-1].startTime.atZone(tz).dayOfWeek != run.startTime.atZone(tz).dayOfWeek)
                sb.append(dateHeaderFormat.format(run.startTime.atZone(tz)))

            if (run.isCurrent || (runTicker.isNotEmpty() && runTicker.size < config.upcomingRuns))
                runTicker.add(run)

            if (run.isCurrent)
                sb.append(">>> \u25B6\uFE0F ")

            sb.append(TimeFormat.DATE_SHORT.format(run.startTime)).append(' ')
            sb.append(TimeFormat.TIME_SHORT.format(run.startTime)).append(": ")

            sb.append(run.name)

            if (run.category.isNotEmpty())// && !"Any%".equals(run.category, true))
                sb.append(" (").append(run.category).append(')')

            if (!run.coop && run.runners.size > 1)
                sb.append(" race")

            if (run.runners.isNotEmpty() || run.runnersAsString.isNotEmpty()) {
                sb.append(" by ")
                if (run.runners.isNotEmpty())
                    naturalJoinTo(sb, run.runners.map { it.name })
                else
                    sb.append(run.runnersAsString)
            }

            if (run.runTime > Duration.ZERO)
                sb.append(" in ").append(run.runTimeText)

            // Append bids
            run.bids.forEach { bid ->
                sb.append('\n')
                if (bid.goal != null) {
                    val percent = bid.donationTotal / bid.goal!!
                    // emoji
                    if (percent >= 1)
                        sb.append("\u2705")
                    else if (bid.state == BidState.OPENED)
                        sb.append("\u26A0\uFE0F")
                    else
                        sb.append("\u274C")

                    // text
                    sb.append(' ').append(bid.name)

                    sb.append(" (")
                        .append(moneyFormatter.format(bid.donationTotal))
                        .append('/')
                        .append(moneyFormatter.format(bid.goal!!))
                        .append(", ")
                        .append((percent * 100).toInt())
                        .append("%)")
                } else {
                    // emoji
                    if (bid.state == BidState.OPENED)
                        sb.append("\u23F2\uFE0F")
                    else
                        sb.append("\uD83D\uDCB0")

                    // text
                    sb.append(' ').append(bid.name)

                    val children = bid.children.sortedByDescending { it.donationTotal }
                    if (children.isNotEmpty()) {
                        sb.append(" (")
                        sb.append("**").append(children.first().name).append("**")
                        if (children.size > 1) {
                            sb.append('/')
                            children.drop(1).joinTo(sb, "/") { it.name }
                        }
                        sb.append(')')
                    }
                }
            }

            // Append VODs
            val vods = run.twitchVODs + run.youtubeVODs
            vods.forEach { vod -> sb.append("\n<").append(vod.asURL()).append('>') }

            // Finalize
            messages.add(MessageTransformer(
                sb.toString(),
                pin = run.isCurrent
            ))
        }

        // Add ticker
        messages.add(MessageTransformer(
            embed = EmbedBuilder {
                title = event.name + " Ticker"
                description = """
                    Bot created by [qixils](https://qixils.dev).
                    Updates every ${config.waitMinutes} minutes.
                    Watch live at [twitch.tv/${config.twitch}](https://twitch.tv/${config.twitch}).
                """.trimIndent()
                color = 0xA547DE // purple
                timestamp = Instant.now()
                footer(name = "Last Updated")

                if (event.datetime > Instant.now()) {
                    field {
                        name = "Starting Soon!"
                        value = "The event will start " +
                                TimeFormat.RELATIVE.format(event.datetime) + " on " +
                                TimeFormat.DATE_TIME_SHORT.format(event.datetime) + "."
                        inline = false
                    }
                } else if (runTicker.isNotEmpty()) {
                    runTicker.forEachIndexed { index, run ->
                        field {
                            name = if (index == 0) "Current Game"
                            else TimeFormat.RELATIVE.format(run.startTime)

                            val sb = StringBuilder(run.name)
                            if (run.category.isNotEmpty())// && !"Any%".equals(run.category, true))
                                sb.append(" (").append(run.category).append(')')
                            if (run.runners.isNotEmpty()) {
                                sb.append(" by ")
                                naturalJoinTo(sb, run.runners.map { runner ->
                                    // TODO: re-add emotes when Discord releases the React Native port for Android
                                    // Find one of the runner's social media profiles
                                    val url: String? = if (runner.stream.isNotEmpty()) {
                                        val twitchMatcher = twitchRegex.matcher(runner.stream)
                                        val urlMatcher = urlRegex.matcher(runner.stream)
                                        if (twitchMatcher.matches())
                                            "https://twitch.tv/${twitchMatcher.group(1)}"
                                        else if (!urlMatcher.matches())
                                            "https://twitch.tv/${runner.stream}"
                                        else
                                            runner.stream
                                    } else if (runner.youtube.isNotEmpty()) {
                                        if (runner.youtube.contains("youtube.com"))
                                            runner.youtube
                                        else if (runner.youtube.startsWith("UC", false))
                                            "https://youtube.com/channel/${runner.youtube}"
                                        else
                                            "https://youtube.com/c/${runner.youtube}"
                                    } else if (runner.twitter.isNotEmpty()) {
                                        if (runner.twitter.contains("twitter.com"))
                                            runner.twitter
                                        else
                                            "https://twitter.com/${runner.twitter}"
                                    } else {
                                        null
                                    }

                                    // Return markdown-formatted name with link if available
                                    if (url != null)
                                        "[${runner.name}]($url)"
                                    else
                                        runner.name
                                })
                            }
                            value = sb.toString()
                            inline = false
                        }
                    }
                } else {
                    field {
                        name = "Thanks for watching!"
                        value = "The event has come to a close. Thank you all for watching and donating!"
                        inline = false
                    }
                }
            },
            pin = false
        ))

        // Send/edit messages
        config.channels.forEach { channelId ->
            // Get channel
            val channel = bot.jda.getTextChannelById(channelId)
            if (channel == null) {
                logger.error("Could not find guild text channel $channelId")
                return@forEach
            }
            // Validate permissions
            val self = channel.guild.selfMember
            if (!self.hasPermission(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)) {
                logger.error("Cannot view and/or send messages in channel $channelId (#${channel.name})")
                return@forEach
            }
            // Get messages
            val channelHistory = channel.history
            val oldMessages = mutableListOf<Message>()
            while (true) {
                val retrievedMessages = channelHistory.retrievePast(100).await().filter {
                    if (it.author.id != bot.jda.selfUser.id)
                        false
                    else
                        it.timeCreated.toInstant().isAfter(event.datetime.minus(1, ChronoUnit.DAYS))
                }
                if (retrievedMessages.isEmpty())
                    break
                oldMessages.addAll(retrievedMessages)
            }
            oldMessages.sortBy { it.timeCreated }
            // Copy message contents for iteration
            val newMessagesCopy = messages.toMutableList()
            // Edit existing messages
            while (oldMessages.isNotEmpty()) {
                val oldMessage = oldMessages.removeFirst()
                if (oldMessage.type != MessageType.DEFAULT) {
                    oldMessage.delete().await()
                    continue
                }
                if (newMessagesCopy.isNotEmpty())
                    newMessagesCopy.removeFirst().edit(oldMessage)
                else
                    oldMessage.delete().await()
            }
            // Send new messages
            newMessagesCopy.forEach { it.send(channel) }
            // Log
            logger.info("Updated schedule for ${config.organization.name}'s ${event.short} in #${channel.name} ($channelId)")
        }
    }
}