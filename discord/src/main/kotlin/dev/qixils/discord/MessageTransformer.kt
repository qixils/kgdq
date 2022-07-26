package dev.qixils.discord

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
    }

    fun send(channel: MessageChannel) {
        channel.sendMessage(message).queue {
            if (pin) it.pin().queue()
        }
    }

    fun edit(toEdit: Message) {
        toEdit.editMessage(message).queue {
            if (!toEdit.guild.selfMember.hasPermission(toEdit.guildChannel, Permission.MESSAGE_MANAGE)) {
                logger.warn("Cannot pin messages in channel ${toEdit.channel.id} (#${toEdit.channel.name}) " +
                        "because bot lacks MANAGE_MESSAGE permission")
            } else {
                (if (pin) it.pin() else it.unpin()).queue()
            }
        }
    }
}
