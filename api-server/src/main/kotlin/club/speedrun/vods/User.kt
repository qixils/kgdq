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
    var role: Role = Role.USER,
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
        rootDb.users.update(this)
    }
    fun clearToken() {
        token = null
        rootDb.users.update(this)
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
        rootDb.getFromDiscord(this) // triggers an update to the database with the new tokens
    }

    private suspend fun user(): DiscordUser {
        refresh()
        return httpClient.get("https://discord.com/api/v10/users/@me") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()
    }

    companion object {
        suspend fun from(data: OAuthAccessTokenResponse.OAuth2): DiscordOAuth {
            val oauth = DiscordOAuth(data.accessToken, data.refreshToken, data.expiresIn, Instant.now().minusSeconds(10).epochSecond)
            oauth.fetchUserOrThrow()
            return oauth
        }
    }
}

@Serializable
data class Profile(
    val id: String,
    val username: String?,
    val role: Role,
    // TODO: accepts/rejects
) {
    companion object {
        suspend fun fromFetch(data: User): Profile {
            return Profile(data.id, data.discord?.fetchUserOrCache()?.username, data.role)
        }

        fun fromCache(data: User): Profile {
            return Profile(data.id, data.discord?.user?.username, data.role)
        }
    }
}

@Serializable
enum class Role {
    BANNED,
    USER,
    APPROVED,
    MODERATOR,
    ADMIN,
}
