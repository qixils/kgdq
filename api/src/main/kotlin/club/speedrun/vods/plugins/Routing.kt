package club.speedrun.vods.plugins

import club.speedrun.vods.*
import club.speedrun.vods.marathon.VOD
import club.speedrun.vods.marathon.VodSuggestion
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

private suspend fun getUser(call: ApplicationCall): User? {
    return try {
        rootDb.getFromSession(call.sessions.get()!!)!!
    } catch (e: Exception) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "You are not authenticated"))
        null
    }
}

private suspend fun getDiscordUser(call: ApplicationCall): DiscordUser? {
    return try {
        rootDb.getFromSession(call.sessions.get()!!)!!.discord!!.fetchUserOrNull()!!
    } catch (e: Exception) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "You are not authenticated"))
        null
    }
}

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
                val redirectUrl = URLBuilder("$root/api/auth/discord/login").run {
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
                get("/user") { call.respond(getDiscordUser(call) ?: return@get) }
            }

            route("/v1") {
                route("/gdq", gdq.route())
                route("/esa", esa.route())
                route("/hek", hek.route())
                route("/rpglb", rpglb.route())

                put("/suggest/vod") {
                    val user = getUser(call) ?: return@put
                    val body: VodSuggestionBody
                    try {
                        body = call.receive()
                    } catch (e: ContentTransformationException) {
                        throw UserError("Invalid request body")
                    }
                    // parse VOD from URL
                    val vod = VOD.fromUrl(body.url)
                    // get marathon
                    val marathon = marathons.firstOrNull { it.api.organization.equals(body.organization, true) }
                        ?: throw UserError("Invalid organization; must be one of: ${marathons.joinToString { it.api.organization }}")
                    // get override
                    val run = marathon.api.db.getRunOverrides(gdqId = body.runId, horaroId = null)
                        ?: throw UserError("Invalid run ID")
                    // add suggestion
                    run.vodSuggestions.add(VodSuggestion(vod, user.id))
                    // update override
                    marathon.api.db.runs.update(run)
                    // respond
                    call.respond(HttpStatusCode.OK)
                }
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

class VodSuggestionBody(
    val organization: String,
    val runId: Int,
    val url: String,
)
