package dev.qixils.discord

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import org.slf4j.LoggerFactory

data class MessageTransformer(
    val message: Message,
    val pin: Boolean = false,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MessageTransformer::class.java)
        private val pinWarnings = mutableSetOf<Long>()
    }

    // this is a `suspend` function because the order of messages is important
    suspend fun send(channel: MessageChannel) {
        val sentMessage = channel.sendMessage(message).await()
        if (pin)
            sentMessage.pin().queue()
    }

    fun edit(toEdit: Message) {
        if (message.embeds.isNotEmpty() || message.contentRaw != toEdit.contentRaw)
            toEdit.editMessage(message).queue()

        if (!toEdit.guild.selfMember.hasPermission(toEdit.guildChannel, Permission.MESSAGE_MANAGE)) {
            if (!pinWarnings.contains(toEdit.guildChannel.idLong)) {
                logger.warn(
                    "Cannot pin messages in channel ${toEdit.channel.id} (#${toEdit.channel.name}) " +
                            "because bot lacks MANAGE_MESSAGE permission"
                )
                pinWarnings.add(toEdit.guildChannel.idLong)
            }
        } else {
            if (pin) {
                if (!toEdit.isPinned)
                    toEdit.pin().queue()
            } else if (toEdit.isPinned) {
                toEdit.unpin().queue()
            }
        }
    }
}
