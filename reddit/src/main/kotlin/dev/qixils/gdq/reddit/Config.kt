package dev.qixils.gdq.reddit

import club.speedrun.vods.marathon.EventData
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
    private val org: String = "GDQ",
    @Setting("thread_id") val threadId: String,
    val youtube: String = "GamesDoneQuick",
    val events: List<EventConfig>,
) {
    @Transient val organization: Organization = Organization.valueOf(org)
}

@ConfigSerializable
data class EventConfig(
    @Setting("display_name") val displayName: String,
    @Setting("event_id") val eventId: String,
    val twitch: String = "GamesDoneQuick",
    val playlist: String? = null,
)

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
        override fun scheduleUrl(event: EventData) = event.horaroUrl
    },
    HEK {
        override val manualVODs = true
        override val displayName = "Hekathon"
        override val homepageUrl = "https://hekathon.com/"
        override fun donateUrl(event: EventData) = "https://hekathon.esamarathon.com/donate/" + event.short
        override fun scheduleUrl(event: EventData) = event.horaroUrl
        override val shortName: String = "Hekathon"
    },
    RPGLB {
        override val manualVODs = true
        override val displayName = "RPG Limit Break"
        override val homepageUrl = "https://rpglimitbreak.com/"
        override fun donateUrl(event: EventData) = "https://rpglimitbreak.com/tracker/ui/donate/" + event.short
        override fun scheduleUrl(event: EventData) = "https://rpglimitbreak.com/tracker/runs/" + event.short
    },
    ;

    abstract val manualVODs: Boolean
    abstract val displayName: String
    abstract val homepageUrl: String
    abstract fun donateUrl(event: EventData): String
    abstract fun scheduleUrl(event: EventData): String
    open val shortName: String get() = name
}