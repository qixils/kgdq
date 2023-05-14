package dev.qixils.gdq

import java.time.Duration

data class CacheType(
    private val id: String,
    val duration: Duration,
    val strict: Boolean = true,
) {
    val model: ModelType<*>
        get() = ModelType.get(id) ?: throw IllegalArgumentException("Unknown model type: $id")

    companion object {

        /**
         * Bids.
         */
        val BID = CacheType("bid", Duration.ofMinutes(5))

        /**
         * Events.
         */
        val EVENT = CacheType("event", Duration.ofMinutes(5))

        /**
         * Speedruns.
         */
        val RUN = CacheType("run", Duration.ofMinutes(5))

        /**
         * Speedrunners.
         */
        val RUNNER = CacheType("runner", Duration.ofDays(1), strict = false)

        /**
         * Headsets.
         */
        val HEADSET = CacheType("headset", Duration.ofDays(1), strict = false)
    }
}