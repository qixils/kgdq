package dev.qixils.gdq.reddit

import club.speedrun.vods.marathon.EventData
import kotlinx.serialization.SerialName
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
    @Setting("display_name") val displayName: String,
    private val org: String = "GDQ",
    @Setting("event_id") val eventId: String,
    @Setting("thread_id") val threadId: String,
    val twitch: String = "GamesDoneQuick",
    val youtube: String = "GamesDoneQuick",
    val playlist: String? = null,
) {
    @Transient val organization: Organization = Organization.valueOf(org)
}

@ConfigSerializable
enum class Organization {
    GDQ {
        override val manualVODs = true
        override val displayName = "Games Done Quick"
        override val homepageUrl = "https://gamesdonequick.com/"
        override fun donateUrl(event: EventData) = "https://gamesdonequick.com/tracker/ui/donate/" + event.short
        override fun scheduleUrl(event: EventData) = "https://gamesdonequick.com/schedule/" + event.id
    },
    ESA {
        override val manualVODs = false
        override val displayName = "European Speedrunner Assembly"
        override val homepageUrl = "https://esamarathon.com/"
        override fun donateUrl(event: EventData) = "https://donations.esamarathon.com/donate/" + event.short
        override fun scheduleUrl(event: EventData) = "https://esamarathon.com/schedule/" // ESA doesn't keep old schedules
    };

    abstract val manualVODs: Boolean
    abstract val displayName: String
    abstract val homepageUrl: String
    abstract fun donateUrl(event: EventData): String
    abstract fun scheduleUrl(event: EventData): String
}