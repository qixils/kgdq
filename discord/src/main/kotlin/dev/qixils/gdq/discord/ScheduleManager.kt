package dev.qixils.gdq.discord

import club.speedrun.vods.client.SvcClient
import club.speedrun.vods.marathon.BidData
import club.speedrun.vods.marathon.EventData
import club.speedrun.vods.marathon.RunData
import club.speedrun.vods.naturalJoinTo
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.EmbedBuilder
import dev.minn.jda.ktx.messages.InlineEmbed
import dev.qixils.gdq.BidState
import dev.qixils.gdq.serializers.DurationAsStringSerializer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.utils.TimeFormat
import org.slf4j.LoggerFactory
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ScheduleManager(
    private val bot: Bot,
    private val config: EventConfig,
) {
    private val db = bot.db.getCollection(ChannelData.serializer(), ChannelData.COLLECTION_NAME)
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val client = SvcClient()
    private val marathonClient = client.getMarathonClient(config.org.lowercase(Locale.ENGLISH))
    private val marathon = runBlocking { marathonClient.get(stats = false) }

    companion object {
        private val logger = LoggerFactory.getLogger(ScheduleManager::class.java)
        private val dateHeaderFormat = DateTimeFormatter.ofPattern("_ _\n> **EEEE** MMM d\n_ _\n", Locale.ENGLISH)
    }

    init {
        logger.debug("Starting schedule manager for ${config.org}'s ${config.id}...")
        scheduler.scheduleAtFixedRate(this::runWrapper, 0, config.waitMinutes, TimeUnit.MINUTES)
    }

    private fun runWrapper() {
        try {
            runBlocking {
                run()
            }
        } catch (e: Exception) {
            logger.error("Error in schedule manager", e)
        }
    }

    private fun appendBidGoalString(sb: StringBuilder, bid: BidData, moneyFormatter: NumberFormat) {
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
    }

    private fun appendBidWarString(sb: StringBuilder, bid: BidData, run: RunData) {
        // emoji
        if (bid.state == BidState.OPENED)
            sb.append("\u23F2\uFE0F")
        else
            sb.append("\uD83D\uDCB0")

        // text
        sb.append(' ').append(bid.name)

        if (bid.children.isNotEmpty()) {
            sb.append(" (")
            val toBold = run.runners.size.coerceAtLeast(1)
            val toDisplay = if (bid.allowUserOptions) toBold else bid.children.size
            bid.children
                .subList(0, toDisplay.coerceAtLeast(5).coerceAtMost(bid.children.size))
                .mapIndexed { index, child -> if (index < toBold) "**${child.name}**" else child.name }
                .joinTo(sb, "/")
            sb.append(')')
        }
    }

    private fun appendBidString(sb: StringBuilder, bid: BidData, run: RunData, moneyFormatter: NumberFormat) {
        sb.append('\n')
        if (bid.goal != null)
            appendBidGoalString(sb, bid, moneyFormatter)
        else
            appendBidWarString(sb, bid, run)
    }

    private fun InlineEmbed.addRunTickerField(index: Int, run: RunData?) {
        field {
            inline = false
            if (run == null || index == 0) {
                name = "Current Game"
                if (run == null) {
                    value = "The event is currently in-between runs. Stay tuned for more!"
                    return@field
                }
            } else {
                name = TimeFormat.RELATIVE.format(run.startTime)
            }

            val sb = StringBuilder(run.name)
            if (!run.category.isNullOrEmpty() && !"Any%".equals(run.category, true))
                sb.append(" (").append(run.category).append(')')
            if (run.runners.isNotEmpty()) {
                sb.append(" by ")
                naturalJoinTo(sb, run.runners) { runner ->
                    // TODO: re-add emotes when Discord releases the React Native port for Android
                    runner.stream?.let { "[${runner.name}]($it)" } ?: runner.name
                }
            }
            value = sb.toString()
        }
    }

    private fun createTickerBody(event: EventData, runTicker: List<RunData?>): InlineEmbed = EmbedBuilder {
        title = event.name + " Ticker"
        description = """
            Bot created by [qixils](https://qixils.dev).
            Updates every ${config.waitMinutes} minutes.
            Watch live at [twitch.tv/${config.twitch}](https://twitch.tv/${config.twitch}).
        """.trimIndent()
        color = 0xA547DE // purple
        timestamp = Instant.now()
        footer(name = "Last Updated")

        if (runTicker.isNotEmpty()) {
            runTicker.forEachIndexed { index, run -> addRunTickerField(index, run) }
        } else if (event.startTime!! > Instant.now()) {
            field {
                name = "Starting Soon!"
                value = "The event will start " +
                        TimeFormat.RELATIVE.format(event.startTime!!) + " on " +
                        TimeFormat.DATE_TIME_SHORT.format(event.startTime!!) + "."
                inline = false
            }
        } else {
            field {
                name = "Thanks for watching!"
                value = "The event has come to a close. Thank you all for watching and donating!"
                inline = false
            }
        }
    }

    private suspend fun updateChannel(channelId: Long, event: EventData, messages: List<MessageTransformer>) {
        // Get channel | TODO: forum support?
        val owningChannel = bot.jda.getTextChannelById(channelId)
        if (owningChannel == null) {
            logger.error("Could not find guild text channel $channelId")
            return
        }
        // Validate permissions
        val self = owningChannel.guild.selfMember
        if (!self.hasPermission(owningChannel, Permission.CREATE_PUBLIC_THREADS)) {
            logger.error("Cannot create threads in channel $channelId (#${owningChannel.name})")
            return
        }
        // Get channel data
        val channelData = db.get(owningChannel.id) ?: ChannelData(owningChannel.id)
        // Get or create thread
        val threadKey = "${config.org}-${event.id}" // TODO: .lowercase() ?
        val thread: ThreadChannel
        if (threadKey in channelData.threads) {
            val threadId = channelData.threads[threadKey]!!
            val pred = { it: ThreadChannel -> it.idLong == threadId }
            thread = owningChannel.threadChannels.firstOrNull(pred)
                ?: owningChannel.retrieveArchivedPublicThreadChannels().firstOrNull(pred)
                ?: run {
                logger.error("Could not find thread ${channelData.threads[threadKey]}")
                return
            }
            if (thread.isArchived)
                thread.manager.setArchived(false).await()
        } else {
            thread = owningChannel.createThreadChannel(event.name)
                .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
                .submit().await()
            channelData.threads[threadKey] = thread.idLong
            db.update(channelData)
        }
        // Validate permissions
        if (!self.hasPermission(thread, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)) {
            logger.error("Cannot view and/or send messages in thread ${thread.id} (#${thread.name})")
            return
        }
        // Get messages
        val channelHistory = thread.history
        val oldMessages = mutableListOf<Message>()
        while (true) {
            val retrievedMessages = channelHistory.retrievePast(100).await().filter { it.author.id == bot.jda.selfUser.id }
            if (retrievedMessages.isEmpty())
                break
            retrievedMessages.forEach { oldMessages.add(0, it) } // reverse order
        }
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
        newMessagesCopy.forEach { it.send(thread) }
        // Log
        logger.info("Updated schedule for ${config.org}'s ${event.short} in #${owningChannel.name} ($channelId)")
    }

    private suspend fun run() = coroutineScope {
        logger.info("Started schedule manager for ${config.org}'s ${config.id}")

        // Get event and run data
        val runs = async { marathonClient.getRuns(event = config.id) }
        val event = marathonClient.getEvent(config.id) ?: throw IllegalStateException("Event ${config.id} by ${config.org} not found")

        // Initialize misc utility vals
        val moneyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        if (event.currency != null) {
            try {
                moneyFormatter.currency = Currency.getInstance(event.currency)
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid currency code ${event.currency} for event ${event.id}", e)
            }
        }

        // Create list of messages
        val messages = mutableListOf<MessageTransformer>()
        val runTicker = mutableListOf<RunData?>()

        // Add header message
        messages.add(
            MessageTransformer(
                "**${event.name}**\n" +
                        "Date headers are in the ${(event.timezone ?: ZoneOffset.UTC).id} timezone.\n" +
                        "Join the ${event.count} donators who have raised " +
                        "${moneyFormatter.format(event.amount)} for ${event.charityName} " +
                        "at <${event.donationUrl}>.",
                pin = true
            )
        )

        // Add message for each run
        runs.await().forEachIndexed { index, run ->
            val sb = StringBuilder()
            val tz = event.timezone ?: ZoneOffset.UTC
            if (index == 0 || runs.await()[index-1].startTime.atZone(tz).dayOfWeek != run.startTime.atZone(tz).dayOfWeek)
                sb.append(dateHeaderFormat.format(run.startTime.atZone(tz)))

            if (run.isCurrent) {
                sb.append(">>> \u25B6\uFE0F ")

                if (Instant.now() < run.startTime)
                    runTicker.add(null)
                runTicker.add(run)
            }
            else if (runTicker.isNotEmpty() && runTicker.size < config.upcomingRuns)
                runTicker.add(run)

            sb.append(TimeFormat.DATE_SHORT.format(run.startTime)).append(' ')
            sb.append(TimeFormat.TIME_SHORT.format(run.startTime)).append(": ")

            sb.append(run.name)

            if (!run.category.isNullOrEmpty() && !"Any%".equals(run.category, true))
                sb.append(" (").append(run.category).append(')')

            if (!run.coop && run.runners.size > 1)
                sb.append(" race")

            if (run.runners.isNotEmpty()) {
                sb.append(" by ")
                naturalJoinTo(sb, run.runners) { it.name }
            }

            if (run.runTime > Duration.ZERO)
                sb.append(" in ").append(DurationAsStringSerializer.format(run.runTime))

            // Append bids
            run.bids.forEach { bid -> appendBidString(sb, bid, run, moneyFormatter) }

            // Append VODs
            run.vods.forEach { vod -> sb.append("\n<").append(vod.url).append('>') }

            // Finalize
            messages.add(
                MessageTransformer(
                    sb.toString(),
                    pin = run.isCurrent
                )
            )
        }

        // Add ticker
        messages.add(
            MessageTransformer(
                embed = createTickerBody(event, runTicker),
                pin = false
            )
        )

        // Send/edit messages
        config.channels.forEach { channelId -> updateChannel(channelId, event, messages) }
    }
}