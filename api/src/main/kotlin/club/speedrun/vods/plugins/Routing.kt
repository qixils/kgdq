@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.plugins

import club.speedrun.vods.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.SerializationException

fun Application.configureRouting() {

    install(Locations) {
    }
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            logError(call, cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to ("An internal error occurred: " + (cause.message ?: cause.toString())))
            )
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You are not authorized to access this resource"))
        }
        exception<AuthenticationException> { call, cause ->
            if (cause.redirect) {
                val redirectUrl = URLBuilder("$root/api/auth/login").run {
                    parameters.append("redirectUrl", root + call.request.uri)
                    build()
                }
                call.respondRedirect(redirectUrl)
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "You are not authenticated"))
            }
        }
        exception<UserError> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
    }


    routing {
        route("/api") {
            route("/auth") {
                get("/test") {
                    val session: DiscordSession? = call.sessions.get()
                    val user = discordUser(session)
                    call.respond(user)
                }
            }

            route("/v1") {
                route("/gdq", gdq.route())
                route("/esa", esa.route())
                route("/hek", hek.route())
                route("/rpglb", rpglb.route())
            }
        }
    }
}


/**
 * Thrown when a user does not have permission to access an endpoint.
 */
class AuthorizationException : RuntimeException()

/**
 * Thrown when a user has attempted to access an authenticated endpoint without logging in.
 */
class AuthenticationException(val redirect: Boolean) : RuntimeException()

/**
 * Thrown when a user enters some invalid input.
 */
class UserError(message: String) : RuntimeException(message)
