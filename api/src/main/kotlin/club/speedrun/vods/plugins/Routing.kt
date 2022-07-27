@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.plugins

import club.speedrun.vods.marathon.ESAMarathon
import club.speedrun.vods.marathon.GDQMarathon
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.SerializationException

val gdq = GDQMarathon()
val esa = ESAMarathon()

fun Application.configureRouting() {
    install(Locations) {
    }
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            logError(call, cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to ("An internal error occurred: " + (cause.message ?: cause.toString()))))
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden)
        }
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
            client = HttpClient(Apache)
        }
    }


    routing {
        route("/api") {
            route("/auth") {
                authenticate("auth-oauth-discord") {
                    get("/login") {
                        // Redirects to 'authorizeUrl' automatically
                    }

                    get("/callback") {
                        val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                        call.sessions.set(UserSession(principal?.accessToken.toString()))
                        call.respondRedirect("/dashboard")
                    }
                }
            }

            route("/v1") {
                route("/gdq", gdq.route())
                route("/esa", esa.route())
            }
        }
    }
}

class AuthorizationException : RuntimeException()
data class UserSession(val accessToken: String)
