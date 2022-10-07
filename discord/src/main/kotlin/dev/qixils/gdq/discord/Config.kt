package dev.qixils.gdq.discord

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class Config(
    val token: String,
    val emotes: EmotesConfig,
    val events: List<EventConfig>,
)

@ConfigSerializable
data class EmotesConfig(
    val twitch: Long?,
    val youtube: Long?,
    val twitter: Long?,
)

@ConfigSerializable
data class EventConfig(
    private val org: String = "GDQ",
    val id: String,
    val channels: List<Long>,
    val twitch: String = "GamesDoneQuick",
    @Setting("upcoming_runs") val upcomingRuns: Int = 5,
    @Setting("wait_minutes") val waitMinutes: Long = 10,
) {
    @Transient val organization: Organization = Organization.valueOf(org)
}

@ConfigSerializable
enum class Organization {
    GDQ,
    ESA,
    HEK,
}
