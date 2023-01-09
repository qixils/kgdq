package club.speedrun.vods

import club.speedrun.vods.db.Identified
import club.speedrun.vods.db.ULID
import club.speedrun.vods.db.nextBytes
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.security.SecureRandom
import java.time.Instant

@Serializable
data class User(
    override val id: String = ULID.random(),
    var token: String? = randomToken(),
    var discord: DiscordOAuth? = null,
    //var reddit: OAuthData? = null,
) : Identified {
    companion object {
        const val COLLECTION_NAME = "users"
        private val random = SecureRandom()
        private fun randomToken(): String = random.nextBytes(32).encodeBase64().dropLastWhile { it == '=' }
    }
    fun regenerateToken() {
        token = randomToken()
    }
    fun clearToken() {
        token = null
    }
    fun session() = UserSession(id, token!!)
}

@Serializable
data class UserSession(
    val id: String,
    val token: String,
)

interface OAuthData {
    val accessToken: String
    val refreshToken: String?
    val expiresIn: Long
    val createdAt: Long
}

@Serializable
data class DiscordOAuth(
    var user: DiscordUser,
    override val accessToken: String,
    override val refreshToken: String,
    override val expiresIn: Long,
    override val createdAt: Long = Instant.now().epochSecond,
) : OAuthData {
    @Transient val expiresAt: Instant = Instant.ofEpochSecond(createdAt).plusSeconds(expiresIn)

    suspend fun fetchUserOrCache(): DiscordUser {
        try {
            user = user(accessToken, refreshToken, expiresAt)
        } catch (_: Exception) {
        }
        return user
    }

    suspend fun fetchUserOrNull(): DiscordUser? {
        return try {
            user = user(accessToken, refreshToken, expiresAt)
            user
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchUserOrThrow(): DiscordUser {
        user = user(accessToken, refreshToken, expiresAt)
        return user
    }

    companion object {
        private suspend fun user(accessToken: String, refreshToken: String, expiresAt: Instant): DiscordUser {
            return httpClient.get("https://discord.com/api/v10/users/@me") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }.body()
        }

        suspend fun from(data: OAuthAccessTokenResponse.OAuth2): DiscordOAuth {
            val user = user(data.accessToken, data.refreshToken!!, Instant.now().plusSeconds(data.expiresIn))
            return DiscordOAuth(user, data.accessToken, data.refreshToken!!, data.expiresIn)
        }
    }
}
