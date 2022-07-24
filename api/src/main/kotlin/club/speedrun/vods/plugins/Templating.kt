package club.speedrun.vods.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.css.*
import kotlinx.html.*

fun Application.configureTemplating() {

    routing {
        get("/html-dsl") {
            call.respondHtml {
                applyTemplate {
                    body {
                        h1(classes = "page-title") { +"HTML" }
                        ul {
                            for (n in 1..10) {
                                li { +"$n" }
                            }
                        }
                    }
                }
            }
        }
        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color.darkBlue
                    margin(0.px)
                }
                rule("h1.page-title") {
                    color = Color.white
                }
            }
        }
    }
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

inline fun applyTemplate(crossinline html: HTML.() -> Unit): HTML.() -> Unit {
    return {
        head {
            link(rel = "stylesheet", href = "/styles.css", type = "text/css")
            // TODO more headers
        }
        // TODO: nav?
        html()
        // TODO: footer?
    }
}

