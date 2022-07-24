package club.speedrun.vods.marathon

import dev.qixils.gdq.models.*
import dev.qixils.gdq.serializers.DurationSerializer
import dev.qixils.gdq.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

@Serializable
data class RunData(
    val id: Int,
    val event: Int,
    val name: String,
    val displayName: String,
    val twitchName: String,
    val console: String,
    val commentators: String,
    val description: String,
    @Serializable(with = InstantSerializer::class) val startTime: Instant,
    @Serializable(with = InstantSerializer::class) val endTime: Instant,
    val order: Int,
    @Serializable(with = DurationSerializer::class) val runTime: Duration,
    @Serializable(with = DurationSerializer::class) val setupTime: Duration,
    val coop: Boolean,
    val category: String,
    val releaseYear: Int?,
    val runners: List<Runner>,
    val bids: List<BidData>,
) {
    // TODO: add parameter for RunOverrides
    constructor(run: Wrapper<Run>, bids: List<BidData>, previousRun: RunData?) : this(
        id = run.id,
        event = run.value.event.id,
        name = run.value.name,
        displayName = run.value.displayName,
        twitchName = run.value.twitchName,
        console = run.value.console,
        commentators = run.value.commentators,
        description = run.value.description,
        startTime = calculateStartTime(run, previousRun),
        endTime = calculateStartTime(run, previousRun) + run.value.runTime,
        order = run.value.order,
        runTime = run.value.runTime, // TODO: use override if available
        setupTime = run.value.setupTime, // TODO: use override if available
        coop = run.value.coop,
        category = run.value.category,
        releaseYear = run.value.releaseYear,
        runners = run.value.runners.map { it.value },
        bids = bids,
    )

    val runTimeText: String get() = DurationSerializer.format(runTime)
    val setupTimeText: String get() = DurationSerializer.format(setupTime)

    companion object {
        private fun calculateStartTime(currentRun: Wrapper<Run>, previousRun: RunData?): Instant {
            return if (previousRun == null) {
                currentRun.value.startTime
            } else {
                previousRun.endTime + currentRun.value.setupTime // TODO: use setupTime override if available
            }
        }
    }
}

@Serializable
data class BidData(
    val children: List<BidData>,
    val name: String,
    val state: BidState,
    val description: String,
    val shortDescription: String,
    val goal: Float?,
    val isTarget: Boolean,
    val allowUserOptions: Boolean,
    val optionMaxLength: Int?,
    @Serializable(with = InstantSerializer::class) val revealedAt: Instant?,
    val donationTotal: Float,
    val donationCount: Int,
    val pinned: Boolean,
) {
    constructor(bid: Bid, children: List<BidData>) : this(
        children = children,
        name = bid.name,
        state = bid.state,
        description = bid.description,
        shortDescription = bid.shortDescription,
        goal = bid.goal,
        isTarget = bid.isTarget,
        allowUserOptions = bid.allowUserOptions,
        optionMaxLength = bid.optionMaxLength,
        revealedAt = bid.revealedAt,
        donationTotal = bid.total,
        donationCount = bid.count,
        pinned = bid.pinned
    )
}