package club.speedrun.vods

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscordUser(
    val id: Long,
    val username: String,
    val discriminator: String,
    val avatar: String? = null,
    val bot: Boolean = false,
    val system: Boolean = false,
    @SerialName("mfa_enabled") val mfaEnabled: Boolean = false,
    val banner: String? = null,
    @SerialName("accent_color") val accentColor: Int? = null,
    val locale: String? = null,
    val verified: Boolean = false,
    val flags: Int? = null,
    @SerialName("premium_type") val premiumType: Int? = null,
    @SerialName("public_flags") val publicFlags: Int? = null,
)
