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
    var accessToken: String
    var refreshToken: String?
    var expiresIn: Long
    var createdAt: Long
    val expiresAt: Instant get() = Instant.ofEpochSecond(createdAt).plusSeconds(expiresIn)
}

@Serializable
data class DiscordOAuth(
    override var accessToken: String,
    override var refreshToken: String?,
    override var expiresIn: Long,
    override var createdAt: Long = Instant.now().epochSecond,
) : OAuthData {

    var user: DiscordUser? = null
    private set

    suspend fun fetchUserOrCache(): DiscordUser? {
        return fetchUserOrNull() ?: user
    }

    suspend fun fetchUserOrNull(): DiscordUser? {
        return try {
            user = user()
            user
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchUserOrThrow(): DiscordUser {
        return fetchUserOrNull()!!
    }

    private suspend fun refresh() {
        if (expiresAt.isAfter(Instant.now())) return
        if (refreshToken == null) return
        createdAt = Instant.now().epochSecond
        val response = httpClient.post("https://discord.com/api/oauth2/token") {
            parameter("client_id", System.getenv("DISCORD_CLIENT_ID"))
            parameter("client_secret", System.getenv("DISCORD_CLIENT_SECRET"))
            parameter("grant_type", "refresh_token")
            parameter("refresh_token", refreshToken)
        }
        val data = response.body<OAuthAccessTokenResponse.OAuth2>()
        accessToken = data.accessToken
        refreshToken = data.refreshToken
        expiresIn = data.expiresIn
    }

    private suspend fun user(): DiscordUser {
        refresh()
        return httpClient.get("https://discord.com/api/v10/users/@me") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()
    }

    companion object {
        fun from(data: OAuthAccessTokenResponse.OAuth2): DiscordOAuth {
            return DiscordOAuth(data.accessToken, data.refreshToken, data.expiresIn, Instant.now().epochSecond)
        }
    }
}
