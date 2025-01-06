@file:OptIn(KtorExperimentalLocationsAPI::class)

package club.speedrun.vods.plugins

import club.speedrun.vods.*
import club.speedrun.vods.marathon.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.time.Duration

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

private fun getMarathon(query: IMarathonRoute): IMarathon {
    return marathons.find { it.id.equals(query.marathon, ignoreCase = true) }
        ?: throw UserError("Unknown organization ${query.marathon}")
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
            logger.warn("Unauthorized access to ${call.request.httpMethod} ${call.request.uri}")
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
            call.respond(cause.statusCode, mapOf("error" to cause.message))
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
        get("/") { call.respond("hello") }

        route("/api") {
//            route("/v1") {
//                route("/gdq", gdq.route())
                //                route("/esa", esa.route())
//                route("/hek", hek.route())
//                route("/rpglb", rpglb.route())
//                route("/bsg", bsg.route())
//            }

            route("/v2") {
                route("/auth") {
                    get("/user") { call.respond(getDiscordUser(call) ?: return@get) } // debug
                }

                route("/marathons") {
                    get<Organization> { _ ->
                        val data = marathons.associate { it.id to it.organizationData }
                        call.respond(data)
                    }

                    get<OrganizationEvents> { query ->
                        val data = mutableMapOf<String, List<EventData>>()
                        coroutineScope { for (marathon in marathons) { launch {
                            val eventData = marathon.getEventsData(query.skipLoad)
                            data[marathon.id] = eventData
                        } } }
                        call.respond(data)
                    }

                    get<MarathonRoute> { query ->
                        val marathon = getMarathon(query)
                        call.respond(marathon.organizationData)
                    }

                    get<EventsRoute> { query ->
                        val marathon = getMarathon(query)
                        call.respond(marathon.getEventsData(query.skipLoad))
                    }

                    get<EventRoute> { query ->
                        val marathon = getMarathon(query)
                        val event = marathon.getEventData(query.event)
                            ?: throw UserError("Unknown event", HttpStatusCode.NotFound)
                        call.respond(event)
                    }

                    get<RunList> { query ->
                        val marathon = getMarathon(query)
                        val runs = marathon.getSchedule(query.event)
                            ?: throw UserError("Unknown event", HttpStatusCode.NotFound)
                        call.respond(runs)
                    }
                }

                get("/events") {
                    val deferreds = marathons.associate { it.id to async { it.getEventsData() } }
                    call.respond(deferreds.mapValues { it.value.await() })
                }

                get<ProfileQuery> { query ->
                    val user: User = if (query.id != null) {
                        rootDb.users.get(query.id) ?: throw UserError("User not found")
                    } else {
                        getUser(call) ?: return@get
                    }
                    call.respond(Profile.fromFetch(user))
                }

                route("/suggest") {
                    put("/vod") {
                        val user = getUser(call) ?: return@put
                        if (user.role <= Role.BANNED)
                            throw AuthorizationException()
                        val body: VodSuggestionBody = call.body()
                        // parse VOD from URL
                        if (body.url.isBlank())
                            throw UserError("VOD URL cannot be blank")
                        val vod = VOD.fromUrl(body.url, contributor = user.id)
                        // get marathon
                        val marathon = marathons.firstOrNull { it.id.equals(body.organization, true) }
                            ?: throw UserError("Invalid organization; must be one of: ${marathons.joinToString { it.id }}")
                        // get run
                        val run = marathon.cacheDb.runs.getByIdForce(body.id)?.obj
                            ?: throw UserError("Unknown run")
                        // get override
                        val override = marathon.overrideDb.getRunOverrides(body.id)
                            ?: throw UserError("Unknown run")
                        val addDirect = user.role < Role.APPROVED || vod.type == VODType.OTHER || override.vods.any { it.type == vod.type }
                        if (addDirect) {
                            // add suggestion if user isn't approved, if the VOD is non-standard, or if VOD might be a duplicate
                            marathon.overrideDb.vodSuggestions.insert(VodSuggestion(vod, marathon.id, override.id))
                        } else {
                            // add VOD
                            override.vods.add(vod)
                        }
                        // update override
                        marathon.overrideDb.runs.update(override)
                        // respond
                        call.respond(HttpStatusCode.OK)
                        // bonus jonas
                        if (webhook.isNotEmpty()) {
                            httpClient.post(webhook) {
                                header("Content-Type", "application/json")
                                setBody(mapOf(
                                    "embeds" to listOf(mapOf(
                                        "title" to "New Submission!",
                                        "fields" to listOf(
                                            mapOf(
                                                "name" to "User",
                                                "value" to "${user.discord?.user?.username ?: "Unknown"} (${user.id})",
                                                "inline" to true,
                                            ),
                                            mapOf(
                                                "name" to "Game",
                                                "value" to "${run.displayGame} (${run.category})",
                                                "inline" to true,
                                            ),
                                            mapOf(
                                                "name" to "Event",
                                                "value" to (marathon.getEventData(run.event)?.name ?: "Unknown"),
                                                "inline" to true,
                                            ),
                                            mapOf(
                                                "name" to "URL",
                                                "value" to vod.url,
                                                "inline" to true,
                                            ),
                                            mapOf(
                                                "name" to "Auto-Accepted",
                                                "value" to addDirect.toString(),
                                                "inline" to true,
                                            ),
                                        ),
                                        "color" to 0xdd22aa,
                                        "url" to "https://vods.speedrun.club/admin",
                                    )),
                                ))
                            }
                        }
                    }

                    delete("/vod") { // ?url=<suggestion_url>
                        val user = getUser(call) ?: return@delete

                        // TODO: lookup by URL is OK, they are unique for a VOD, but is not straightforward when it is
                        //  not the primary key.
                        val url = call.parameters["url"] ?: throw UserError("Missing suggestion URL parameter")
                        var marathon: IMarathon? = null
                        var vod: VOD? = null
                        var run: RunOverrides? = null
                        for (marathon_ in marathons) {
                            for (run_ in marathon_.overrideDb.runs.getAll()) {
                                for (vod_ in run_.vods) {
                                    if (vod_.url == url) {
                                        marathon = marathon_
                                        vod = vod_
                                        run = run_
                                        break
                                    }
                                }
                            }
                        }
                        if (marathon == null || vod == null || run == null)
                            throw UserError("Invalid suggestion URL: $url")

                        // Admins OR the contributor are allowed
                        if (!(user.role >= Role.MODERATOR || vod.contributorId == user.id))
                            throw AuthorizationException()

                        run.vods.remove(vod)

                        // update override
                        marathon.overrideDb.runs.update(run)
                        // respond
                        call.respond(HttpStatusCode.OK)
                    }
                }
                
                route("/list") {
                    get("/suggestions") {
                        val user = getUser(call) ?: return@get
                        if (user.role < Role.MODERATOR)
                            throw AuthorizationException()

                        val suggestions: List<SuggestionWrapper> = marathons
                            .flatMap { marathon -> marathon.overrideDb.vodSuggestions.getAll().map { SuggestionWrapper(it, marathon.id) } }
                        call.respond(suggestions)
                    }
                }

                route("/set") {
                    put("/time") {
                        val user = getUser(call) ?: return@put
                        if (user.role < Role.MODERATOR)
                            throw AuthorizationException()
                        val body: SetTimeBody = call.body()
                        // parse duration from time
                        val duration = Duration.ofSeconds(body.time)
                        // get marathon
                        val marathon = marathons.firstOrNull { it.id.equals(body.organization, true) }
                            ?: throw UserError("Invalid organization; must be one of: ${marathons.joinToString { it.id }}")
                        // get override
                        val run = marathon.overrideDb.getRunOverrides(body.id)
                            ?: throw UserError("Invalid run ID")
                        // set time
                        run.runTime = duration
                        // update override
                        marathon.overrideDb.runs.update(run)
                        // respond
                        call.respond(HttpStatusCode.OK)
                    }

                    put("/suggestion") {
                        val user = getUser(call) ?: return@put
                        if (user.role < Role.MODERATOR)
                            throw AuthorizationException()
                        val body: ModifySuggestionBody = call.body()

                        for (marathon in marathons) {
                            val suggestion = marathon.overrideDb.vodSuggestions.get(body.id) ?: continue
                            suggestion.state = body.action
                            marathon.overrideDb.vodSuggestions.update(suggestion)

                            if (body.action == VodSuggestionState.APPROVED) {
                                marathon.overrideDb.getRunOverrides(suggestion.runId)?.let {
                                    it.vods.add(suggestion.vod)
                                    marathon.overrideDb.runs.update(it)
                                }
                            }
                            break
                        }

                        // respond
                        call.respond(HttpStatusCode.OK)
                    }

                    put("/role") {
                        val user = getUser(call) ?: return@put
                        if (user.role < Role.ADMIN)
                            throw AuthorizationException()
                        val body: ModifyRoleBody = call.body()
                        val target = rootDb.users.get(body.id) ?: throw UserError("User not found")
                        target.role = body.role
                        rootDb.users.update(target)
                        call.respond(HttpStatusCode.OK)
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
class UserError(message: String, val statusCode: HttpStatusCode = HttpStatusCode.BadRequest) : RuntimeException(message)

interface RunBasedBody {
    val organization: String
    val id: String
}

@Serializable
class VodSuggestionBody(
    val url: String,
    override val organization: String,
    override val id: String,
) : RunBasedBody

@Serializable
class SetTimeBody(
    val time: Long,
    override val organization: String,
    override val id: String,
) : RunBasedBody

@Serializable
class SuggestionWrapper(
    val vod: VOD,
    override val id: String,
    override val organization: String,
) : RunBasedBody {
    constructor(suggestion: VodSuggestion, organization: String)
            : this(suggestion.vod, suggestion.id, organization)
}

@Serializable
class ModifySuggestionBody(
    val id: String,
    val action: VodSuggestionState,
)

@Serializable
class ModifyRoleBody(
    val id: String,
    val role: Role,
)

@Location("/profile")
data class ProfileQuery(
    val id: String? = null,
)

@Location("")
data class Organization(val stats: Boolean = true)

@Location("/events")
data class OrganizationEvents(val skipLoad: Boolean = true)

interface IMarathonRoute { val marathon: String }

@Location("/{marathon}")
data class MarathonRoute(override val marathon: String) : IMarathonRoute
@Location("/{marathon}/events")
data class EventsRoute(override val marathon: String, val skipLoad: Boolean = false) : IMarathonRoute
@Location("/{marathon}/events/{event}")
data class EventRoute(override val marathon: String, val event: String, val skipLoad: Boolean = false) : IMarathonRoute
@Location("/{marathon}/events/{event}/runs")
data class RunList(override val marathon: String, val event: String) : IMarathonRoute

suspend inline fun <reified T> ApplicationCall.body(): T {
    try {
        return receive()
    } catch (e: ContentTransformationException) {
        var message = "Invalid request body"
        e.message?.let { message += ": $it" }
        throw UserError(message)
    }
}
