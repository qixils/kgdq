@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.plugins

import club.speedrun.vods.marathon.ESAMarathon
import club.speedrun.vods.marathon.GDQMarathon
import io.ktor.http.*
import io.ktor.server.application.*
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
            call.respond(HttpStatusCode.InternalServerError,
                mapOf("error" to ("An internal error occurred: " + (cause.message ?: cause.toString()))))
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden)
        }
        exception<AuthenticationException> { call, cause ->
            if (cause.redirect) {
                call.respondRedirect("/api/auth/login")
                // TODO: auto-redirect to the page that caused the error? (idk how to do that)
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }


    routing {
        route("/api") {
            route("/auth") {
                get("/test") {
                    val session = call.sessions.get<UserSession>()
                    val user = discordUser(session)
                    call.respond(user)
                }
            }

            route("/v1") {
                route("/gdq", gdq.route())
                route("/esa", esa.route())
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
