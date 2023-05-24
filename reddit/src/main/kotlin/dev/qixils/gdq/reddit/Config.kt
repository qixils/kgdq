package dev.qixils.gdq.reddit

import net.dean.jraw.oauth.Credentials
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class Config(
    val credentials: CredentialConfig,
    @Setting("wait_minutes") val waitMinutes: Long = 10,
    val threads: List<ThreadConfig>,
)

@ConfigSerializable
data class CredentialConfig(
    val username: String,
    val password: String,
    @Setting("client_id") val clientId: String,
    @Setting("client_secret") val clientSecret: String,
) {
    fun toCredentials() = Credentials.script(username, password, clientId, clientSecret)
}

@ConfigSerializable
data class ThreadConfig(
    val org: String = "GDQ",
    @Setting("thread_id") val threadId: String,
    val youtube: String = "GamesDoneQuick",
    val events: List<EventConfig>,
)

@ConfigSerializable
data class EventConfig(
    @Setting("display_name") val displayName: String,
    @Setting("event_id") val eventId: String,
    val twitch: String = "GamesDoneQuick",
    val playlist: String? = null,
)