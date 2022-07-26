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
        // TODO: oauth error stuff? (if not already handled by the oauth plugin ig)
//        exception<AuthenticationException> { call, cause ->
//            call.respond(HttpStatusCode.Unauthorized)
//        }
        exception<AuthorizationException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden)
        }
    }
//    install(Webjars) {
//        path = "/webjars" //defaults to /webjars
//    }

    routing {
        route("/api/v1") {
            route("/gdq", gdq.route())
            route("/esa", esa.route())
        }

        // TODO remove all this sample stuff
        get("/") {
            call.respondText("Hello World!")
        }
        get<MyLocation> {
            call.respondText("Location: name=${it.name}, arg1=${it.arg1}, arg2=${it.arg2}")
        }
        // Register nested routes
        get<Type.Edit> {
            call.respondText("Inside $it")
        }
        get<Type.List> {
            call.respondText("Inside $it")
        }
        get("/webjars") {
            call.respondText(
                "<script src='/webjars/jquery/jquery.js'></script>",
                ContentType.Text.Html
            )
        }
    }
}

@Location("/location/{name}")
class MyLocation(val name: String, val arg1: Int = 42, val arg2: String = "default")
@Location("/type/{name}")
data class Type(val name: String) {
    @Location("/edit")
    data class Edit(val type: Type)

    @Location("/list/{page}")
    data class List(val type: Type, val page: Int)
}

//class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
