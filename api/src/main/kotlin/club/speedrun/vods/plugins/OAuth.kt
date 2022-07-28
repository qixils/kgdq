package club.speedrun.vods.plugins

import club.speedrun.vods.DiscordUser
import club.speedrun.vods.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import java.time.Instant

fun Application.configureOAuth() {
    install (Sessions) {
        cookie<UserSession>("user_session")
    }
    authentication {
        oauth("auth-oauth-discord") {
            urlProvider = { "https://vods.speedrun.club/api/auth/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "discord",
                    authorizeUrl = "https://discord.com/api/oauth2/authorize",
                    accessTokenUrl = "https://discord.com/api/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("DISCORD_CLIENT_ID"),
                    clientSecret = System.getenv("DISCORD_CLIENT_SECRET"),
                    defaultScopes = listOf("identify"),
                )
            }
            client = httpClient
        }
    }

    routing {
        route("/api/auth") {
            authenticate("auth-oauth-discord") {
                get("/login") {
                    // Redirects to 'authorizeUrl' automatically
                }

                get("/callback") {
                    val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }
                    call.sessions.set(UserSession(
                        principal.accessToken,
                        principal.refreshToken,
                        principal.expiresIn,
                        Instant.now().toString(),
                    ))
                    call.respondRedirect("/api/auth/test")
                }
            }
        }
    }
}

suspend fun discordUser(userSession: UserSession?, redirectOnFailure: Boolean = true): DiscordUser {
    if (userSession == null)
        throw AuthenticationException(redirectOnFailure)

    // TODO: support refreshing tokens
    return httpClient.get("https://discord.com/api/v10/users/@me") {
        header(HttpHeaders.Authorization, "Bearer ${userSession.accessToken}")
    }.body()
}

@Serializable
data class UserSession(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long,
    val createdAt: String,
) {
    fun expiresAt(): Instant {
        return Instant.parse(createdAt).plusSeconds(expiresIn)
    }
}
