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
    suspend fun send(channel: MessageChannel): Message {
        val sentMessage = channel.send(content, embeds = embeds).await()
        if (pin)
            sentMessage.pin().await()
        return sentMessage
    }

    suspend fun edit(toEdit: Message): Message {
        val edited
        = if (content != toEdit.contentRaw || toEdit.embeds.isNotEmpty() || embeds.isNotEmpty())
            toEdit.edit(content, embeds = embeds, replace = true).await()
        else
            toEdit

        if (!edited.guild.selfMember.hasPermission(edited.guildChannel, Permission.MESSAGE_MANAGE)) {
            if (!pinWarnings.contains(edited.guildChannel.idLong)) {
                logger.warn(
                    "Cannot pin messages in channel ${edited.channel.id} (#${edited.channel.name}) " +
                            "because bot lacks MANAGE_MESSAGE permission"
                )
                pinWarnings.add(edited.guildChannel.idLong)
            }
        } else if (pin) {
            if (!edited.isPinned)
                edited.pin().await()
        } else if (edited.isPinned) {
            edited.unpin().await()
        }
        return edited
    }
}
