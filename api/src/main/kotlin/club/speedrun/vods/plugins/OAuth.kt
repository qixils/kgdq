package club.speedrun.vods.plugins

import club.speedrun.vods.DiscordUser
import club.speedrun.vods.httpClient
import club.speedrun.vods.root
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
    val redirects = mutableMapOf<String, String>()

    install(Sessions) {
        cookie<DiscordSession>("discord_session")
    }

    install(Authentication) {
        oauth("auth-oauth-discord") {
            urlProvider = { "$root/api/auth/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "discord",
                    authorizeUrl = "https://discord.com/api/oauth2/authorize",
                    accessTokenUrl = "https://discord.com/api/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = System.getenv("DISCORD_CLIENT_ID"),
                    clientSecret = System.getenv("DISCORD_CLIENT_SECRET"),
                    defaultScopes = listOf("identify"),
                    onStateCreated = { call, state ->
                        redirects[state] = call.request.queryParameters["redirectUrl"]!!
                    }
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
                    val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "You are not authenticated"))
                        return@get
                    }
                    call.sessions.set(
                        DiscordSession(
                            principal.accessToken,
                            principal.refreshToken,
                            principal.expiresIn,
                            Instant.now().epochSecond,
                        )
                    )
                    val redirect = redirects[principal.state!!]
                    call.respondRedirect(redirect!!)
                }
            }
        }
    }
}

suspend fun discordUser(discordSession: DiscordSession?, redirectOnFailure: Boolean = true): DiscordUser {
    if (discordSession == null)
        throw AuthenticationException(redirectOnFailure)

    // TODO: support refreshing tokens
    return httpClient.get("https://discord.com/api/v10/users/@me") {
        header(HttpHeaders.Authorization, "Bearer ${discordSession.accessToken}")
    }.body()
}

@Serializable
data class DiscordSession(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long,
    val createdAt: Long,
) {
    fun expiresAt(): Instant {
        return Instant.ofEpochSecond(createdAt).plusSeconds(expiresIn)
    }
}
