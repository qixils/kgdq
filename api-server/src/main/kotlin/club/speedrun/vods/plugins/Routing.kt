@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.plugins

import club.speedrun.vods.*
import club.speedrun.vods.marathon.Organization
import club.speedrun.vods.marathon.VOD
import club.speedrun.vods.marathon.VodSuggestion
import club.speedrun.vods.marathon.VodSuggestionState
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.time.Duration

private val ADMINS: List<String> = listOf("01GSPA63RW82QK1KAA4TPX1D20", "01H1FNG7BG6VQX1ETDC3ZY13D6")

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
        rootDb.getFromSession(call.sessions.get()!!)!!.discord!!.fetchUserOrThrow()
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
            logger.error("Serialization error on ${call.request.httpMethod} ${call.request.uri}", cause)
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
                val redirectUrl = URLBuilder("$root/api/v2/auth/discord/login").run {
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
        exception<Throwable> { call, cause ->
            logger.error("Internal server error on ${call.request.httpMethod} ${call.request.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "An internal error occurred")
            )
        }
    }


    routing {
        route("/api") {
            route("/v1") {
                route("/gdq", gdq.route())
                route("/esa", esa.route())
                route("/hek", hek.route())
                route("/rpglb", rpglb.route())
            }

            route("/v2") {
                route("/auth") {
                    get("/user") { call.respond(getDiscordUser(call) ?: return@get) } // debug
                }

                route("/marathons") {
                    get<Organization> {query ->
                        val deferreds = marathons.associate { it.id to async { it.getOrganizationData(query) } }
                        call.respond(deferreds.mapValues { it.value.await() })
                    }

                    marathons.forEach {
                        route("/${it.id}", it.route())
                    }

                    get("/events") {
                        val deferreds = marathons.associate { it.id to async { it.getEventsData() } }
                        call.respond(deferreds.mapValues { it.value.await() })
                    }
                }

                get("/profile") {
                    val user = getUser(call) ?: return@get
                    call.respond(mapOf("id" to user.id, "name" to user.discord?.user?.username))
                }

                route("/suggest") {
                    put("/vod") {
                        val user = getUser(call) ?: return@put
                        val body: VodSuggestionBody = call.body()
                        // parse VOD from URL
                        val vod = VOD.fromUrl(body.url, contributor = user.id)
                        // get marathon
                        val marathon = marathons.firstOrNull { it.id.equals(body.organization, true) }
                            ?: throw UserError("Invalid organization; must be one of: ${marathons.joinToString { it.id }}")
                        // get override
                        val run = marathon.db.getRunOverrides(gdqId = body.gdqId, horaroId = body.horaroId)
                            ?: throw UserError("Invalid run ID")
                        // add suggestion
                        run.vodSuggestions.add(VodSuggestion(vod))
                        // update override
                        marathon.db.runs.update(run)
                        // respond
                        call.respond(HttpStatusCode.OK)
                    }
                }

                route("/add") {
                    put("/vod") {
                        val user = getUser(call) ?: return@put
                        if (user.id !in ADMINS)
                            throw AuthorizationException()
                        val body: VodSuggestionBody = call.body()
                        // parse VOD from URL
                        val vod = VOD.fromUrl(body.url)
                        // get marathon
                        val marathon = marathons.firstOrNull { it.id.equals(body.organization, true) }
                            ?: throw UserError("Invalid organization; must be one of: ${marathons.joinToString { it.id }}")
                        // get override
                        val run = marathon.db.getRunOverrides(gdqId = body.gdqId, horaroId = body.horaroId)
                            ?: throw UserError("Invalid run ID")
                        // add VOD
                        run.vods.add(vod)
                        // update override
                        marathon.db.runs.update(run)
                        // respond
                        call.respond(HttpStatusCode.OK)
                    }
                }
                
                route("/list") {
                    get("/suggestions") {
                        val user = getUser(call) ?: return@get
                        if (user.id !in ADMINS)
                            throw AuthorizationException()
                        val suggestions: List<SuggestionWrapper> = marathons.flatMap { marathon ->
                            marathon.db.runs.getAll().flatMap { run ->
                                run.vodSuggestions.filter { it.state == VodSuggestionState.PENDING }.map { suggestion ->
                                    SuggestionWrapper(suggestion, marathon.id, run.runId, run.horaroId) } } }
                        call.respond(suggestions)
                    }
                }

                route("/set") {
                    put("/time") {
                        val user = getUser(call) ?: return@put
                        if (user.id !in ADMINS)
                            throw AuthorizationException()
                        val body: SetTimeBody = call.body()
                        // validate body
                        if (body.gdqId == null && body.horaroId == null)
                            throw UserError("Must specify either GDQ ID or Horaro ID")
                        // parse duration from time
                        val duration = Duration.ofSeconds(body.time)
                        // get marathon
                        val marathon = marathons.firstOrNull { it.id.equals(body.organization, true) }
                            ?: throw UserError("Invalid organization; must be one of: ${marathons.joinToString { it.id }}")
                        // get override
                        val run = marathon.db.getRunOverrides(gdqId = body.gdqId, horaroId = body.horaroId)
                            ?: throw UserError("Invalid run ID")
                        // set time
                        run.runTime = duration
                        // update override
                        marathon.db.runs.update(run)
                        // respond
                        call.respond(HttpStatusCode.OK)
                    }

                    put("/suggestion") {
                        val user = getUser(call) ?: return@put
                        if (user.id !in ADMINS)
                            throw AuthorizationException()
                        val body: ModifySuggestionBody = call.body()
                        for (marathon in marathons) {
                            for (run in marathon.db.runs.getAll()) {
                                for (suggestion in run.vodSuggestions) {
                                    if (suggestion.id == body.id) {
                                        suggestion.state = body.action
                                        if (body.action == VodSuggestionState.APPROVED) {
                                            run.vods.add(suggestion.vod)
                                        }
                                        // update override
                                        marathon.db.runs.update(run)
                                        // respond
                                        call.respond(HttpStatusCode.OK)
                                        return@put
                                    }
                                }
                            }
                        }
                        throw UserError("Invalid suggestion ID")
                    }
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

interface RunBasedBody {
    val organization: String
    val gdqId: Int?
    val horaroId: String?
}

@Serializable
class VodSuggestionBody(
    val url: String,
    override val organization: String,
    override val gdqId: Int? = null,
    override val horaroId: String? = null,
) : RunBasedBody

@Serializable
class SetTimeBody(
    val time: Long,
    override val organization: String,
    override val gdqId: Int? = null,
    override val horaroId: String? = null,
) : RunBasedBody

@Serializable
class SuggestionWrapper(
    val vod: VOD,
    val id: String,
    override val organization: String,
    override val gdqId: Int?,
    override val horaroId: String?,
) : RunBasedBody {
    constructor(suggestion: VodSuggestion, organization: String, gdqId: Int?, horaroId: String?)
            : this(suggestion.vod, suggestion.id, organization, gdqId, horaroId)
}

@Serializable
class ModifySuggestionBody(
    val id: String,
    val action: VodSuggestionState,
)

suspend inline fun <reified T> ApplicationCall.body(): T {
    try {
        return receive()
    } catch (e: ContentTransformationException) {
        var message = "Invalid request body"
        e.message?.let { message += ": $it" }
        throw UserError(message)
    }
}
