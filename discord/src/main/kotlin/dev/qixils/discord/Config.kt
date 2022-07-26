package dev.qixils.discord

import org.spongepowered.configurate.objectmapping.ConfigSerializable

// todo these may need to be var

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
    val org: Organization = Organization.GDQ,
    val id: String = "latest",
    val channels: List<Long>,
    val twitch: String = "GamesDoneQuick",
    val upcomingRuns: Int = 5,
    val waitMinutes: Long = 10,
)

@ConfigSerializable
enum class Organization {
    GDQ,
    ESA,
}
