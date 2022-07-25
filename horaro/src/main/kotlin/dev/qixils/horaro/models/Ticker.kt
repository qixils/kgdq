@file:OptIn(InternalHoraroApi::class)

package dev.qixils.horaro.models

import dev.qixils.horaro.InternalHoraroApi
import kotlinx.serialization.Serializable

/**
 * A ticker is an abbreviated version of a [BaseSchedule] which contains only the previous, current,
 * and next run.
 *
 * @param schedule the metadata of the schedule this ticker is based on
 * @param ticker   the body of the ticker
 * @param links    related links
 */
@Serializable
data class Ticker(

    /**
     * The metadata of the schedule this ticker is based on.
     */
    val schedule: PartialSchedule,

    /**
     * The body of this ticker.
     */
    val ticker: TickerBody,

    /**
     * A collection of related links.
     */
    override val links: List<Link>,
) : Linkable {
    init {
        val columns = schedule.columns
        ticker.previous?.initData(columns)
        ticker.current?.initData(columns)
        ticker.next?.initData(columns)
    }
}

/**
 * The primary body of a ticker.
 *
 * @param previous the previous run
 * @param current  the current run
 * @param next     the next run
 */
@Serializable
data class TickerBody(

    /**
     * The previous run on the schedule.
     */
    val previous: Run?,

    /**
     * The current run on the schedule.
     */
    val current: Run?,

    /**
     * The next run on the schedule.
     */
    val next: Run?,
)
