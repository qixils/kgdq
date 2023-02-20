package club.speedrun.vods.plugins

import club.speedrun.vods.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlin.collections.set

fun Application.configureOAuth() {
    val redirects = mutableMapOf<String, String>()

    install(Sessions) {
        cookie<UserSession>("user_session")
    }

    install(Authentication) {
        oauth("auth-oauth-discord") {
            urlProvider = { "$root/api/auth/discord/callback" }
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
                        call.request.queryParameters["redirectUrl"]?.let { redirects[state] = it }
                    }
                )
            }
            client = httpClient
        }
    }

    routing {
        route("/api/auth/discord") {
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
                    val oauth = DiscordOAuth.from(principal)
                    val user = rootDb.getOrCreateFromDiscord(oauth)
                    call.sessions.set(user.session())
                    val redirect = redirects[principal.state!!] ?: "/"
                    call.respondRedirect(redirect)
                }
            }
        }
    }
}
