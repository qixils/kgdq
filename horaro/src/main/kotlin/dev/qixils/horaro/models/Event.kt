package dev.qixils.horaro.models

import dev.qixils.horaro.Horaro
import kotlinx.serialization.Serializable

/**
 * Events are the owners of [marathon schedules][FullSchedule] and contain information about what
 * the event is, who organizes it, and where you can find its online presence.
 *
 * @constructor Creates a new event.
 * @property id          the id of the event
 * @property name        the name of the event
 * @property slug        the slug (short name) of the event
 * @property link        the link to the event's Horaro page
 * @property description the description of the event; may be null
 * @property owner       the name of the event's owner
 * @property website     the event's personal website; may be null
 * @property twitter     the event's Twitter handle; may be null
 * @property twitch      the name of the event's Twitch channel; may be null
 */
@Serializable
data class Event(

    /**
     * The id of the event.
     */
    override val id: String,

    /**
     * The name of the event.
     */
    override val name: String,

    /**
     * The slug (short name) of the event.
     */
    override val slug: String,

    /**
     * The link to the event's Horaro page.
     */
    override val link: String,

    /**
     * The description of the event. May be null.
     */
    val description: String?,

    /**
     * The name of the event's owner.
     */
    val owner: String,

    /**
     * The website of the event. May be null.
     */
    val website: String?,

    /**
     * The Twitter handle of the event. May be null.
     */
    val twitter: String?,

    /**
     * The name of the event's Twitch channel. May be null.
     */
    val twitch: String?,
) : Identifiable {

    /**
     * Fetches this event's schedules.
     *
     * @param offset the offset of the schedules to fetch
     * @return a [ScheduleResponse] containing the event's [FullSchedule]s
     */
    suspend fun getSchedules(offset: Int = 0): ScheduleResponse {
        return Horaro.getSchedules(this, offset)
    }

    /**
     * Fetches the schedule with the provided [slug].
     *
     * @param slug      the slug (or ID) of the schedule to fetch
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the requested [FullSchedule] if found, null otherwise
     */
    suspend fun getSchedule(slug: String, hiddenKey: String? = null): FullSchedule? {
        return Horaro.getSchedule(this, slug, hiddenKey)
    }

    /**
     * Fetches the ticker with the provided schedule [slug].
     *
     * @param slug the slug (or ID) of the schedule to fetch the ticker from
     * @param hiddenKey optional: the key used to include hidden columns if a "hidden column secret"
     *                            is configured
     * @return the requested [Ticker] if found, null otherwise
     */
    suspend fun getTicker(slug: String, hiddenKey: String? = null): Ticker? {
        return Horaro.getTicker(this, slug, hiddenKey)
    }
}
