package dev.qixils.gdq.discord

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineEmbed
import dev.minn.jda.ktx.messages.edit
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.slf4j.LoggerFactory

data class MessageTransformer(
    val content: String = "",
    val embeds: Collection<MessageEmbed> = emptyList(),
    val pin: Boolean = false,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MessageTransformer::class.java)
        private val pinWarnings = mutableSetOf<Long>()
    }

    constructor(
        content: String = "",
        embed: InlineEmbed,
        pin: Boolean = false,
    ) : this(content, listOf(embed.build()), pin)

    // this is a `suspend` function because the order of messages is important
    suspend fun send(channel: MessageChannel) {
        val sentMessage = channel.send(content, embeds = embeds).await()
        if (pin)
            sentMessage.pin().await()
    }

    suspend fun edit(toEdit: Message) {
        if (content != toEdit.contentRaw || toEdit.embeds.isNotEmpty() || embeds.isNotEmpty())
            toEdit.edit(content, embeds = embeds, replace = true).await()

        if (!toEdit.guild.selfMember.hasPermission(toEdit.guildChannel, Permission.MESSAGE_MANAGE)) {
            if (!pinWarnings.contains(toEdit.guildChannel.idLong)) {
                logger.warn(
                    "Cannot pin messages in channel ${toEdit.channel.id} (#${toEdit.channel.name}) " +
                            "because bot lacks MANAGE_MESSAGE permission"
                )
                pinWarnings.add(toEdit.guildChannel.idLong)
            }
        } else if (pin) {
            if (!toEdit.isPinned)
                toEdit.pin().await()
        } else if (toEdit.isPinned) {
            toEdit.unpin().await()
        }
    }
}
