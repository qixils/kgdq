package club.speedrun.vods.igdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwitchToken(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("token_type")
    val tokenType: String,
) {
    val authorization = "${tokenType[0].uppercase()}${tokenType.substring(1)} $accessToken"
    val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)

    val isExpired
        get() = System.currentTimeMillis() > expiresAt
}
