package dev.qixils.horaro.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A link to a related API resource.
 *
 * @constructor Creates a new link.
 * @param uri  the link's URI
 * @param type the link's type
 */
@Serializable
data class Link(

    /**
     * The link's URI.
     */
    val uri: String,

    /**
     * The link's type.
     */
    @SerialName("rel") val type: LinkType,
)

/**
 * The unique type of link.
 */
@Serializable
enum class LinkType {

    /**
     * The self link is a permalink to the owning resource.
     */
    SELF,

    /**
     * The link to a schedule's ticker.
     */
    TICKER,

    /**
     * The link to a schedule's event.
     */
    EVENT,

    /**
     * The link to a collection of events.
     */
    EVENTS,

    /**
     * The link to an event's schedule.
     */
    SCHEDULE,

    /**
     * The link to a collection of schedules.
     */
    SCHEDULES,

    /**
     * The link to the previous page of a paginated response.
     */
    PREV,

    /**
     * The link to the next page of a paginated response.
     */
    NEXT,
}

/**
 * An object that contains a collection of links.
 */
interface Linkable {

    /**
     * A collection of related links.
     */
    val links: List<Link>
}
