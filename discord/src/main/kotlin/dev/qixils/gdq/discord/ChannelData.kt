package dev.qixils.gdq.discord

import club.speedrun.vods.db.Identified
import kotlinx.serialization.Serializable

@Serializable
data class ChannelData(
    override val id: String, // channel ID
    val threads: MutableMap<String, Long> = mutableMapOf() // thread IDs
) : Identified {
    companion object {
        const val COLLECTION_NAME = "channels"
    }
}
