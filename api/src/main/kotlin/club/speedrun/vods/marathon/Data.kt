package club.speedrun.vods.marathon

import dev.qixils.gdq.models.*
import dev.qixils.gdq.serializers.DurationAsStringSerializer
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@Serializable
data class RunData(
    @Transient private val source: Run? = null,
    val id: Int,
    val event: Int,
    val name: String,
    val displayName: String,
    val twitchName: String,
    val console: String,
    val commentators: String,
    val description: String,
    @Serializable(with = InstantAsStringSerializer::class) val startTime: Instant,
    @Serializable(with = InstantAsStringSerializer::class) val endTime: Instant,
    val order: Int,
    @Serializable(with = DurationAsStringSerializer::class) val runTime: Duration,
    @Serializable(with = DurationAsStringSerializer::class) val setupTime: Duration,
    val coop: Boolean,
    val category: String,
    val releaseYear: Int?,
    val runners: MutableList<Runner>,
    val bids: List<BidData>,
) {
    // TODO: add parameter for RunOverrides
    constructor(run: Wrapper<Run>, bids: List<BidData>, previousRun: RunData?) : this(
        source = run.value,
        id = run.id,
        event = run.value.eventId,
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
        runners = mutableListOf(),
        bids = bids,
    )

    suspend fun loadRunners() {
        runners.addAll(source!!.runners().map { it.value })
    }

    val runTimeText: String get() = DurationAsStringSerializer.format(runTime)
    val setupTimeText: String get() = DurationAsStringSerializer.format(setupTime)

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
    // TODO: id?
    val children: List<BidData>,
    val name: String,
    val state: BidState,
    val description: String,
    val shortDescription: String,
    val goal: Float?,
    val isTarget: Boolean,
    val allowUserOptions: Boolean,
    val optionMaxLength: Int?,
    @Serializable(with = InstantAsStringSerializer::class) val revealedAt: Instant?,
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

@Serializable
data class EventData(
    val id: Int,
    val short: String,
    val name: String,
    val hashtag: String,
    val charityName: String,
    val targetAmount: Float,
    val minimumDonation: Float,
    val paypalCurrency: String,
    @Serializable(with = InstantAsStringSerializer::class) val datetime: Instant,
    @Serializable(with = ZoneIdSerializer::class) val timezone: ZoneId,
    val locked: Boolean,
    val allowDonations: Boolean,
    val canonicalUrl: String,
    val public: String,
    val amount: Float,
    val count: Int,
    val max: Float,
    val avg: Double,
) {
    constructor(event: Wrapper<Event>) : this(
        id = event.id,
        short = event.value.short,
        name = event.value.name,
        hashtag = event.value.hashtag,
        charityName = event.value.charityName,
        targetAmount = event.value.targetAmount,
        minimumDonation = event.value.minimumDonation,
        paypalCurrency = event.value.paypalCurrency,
        datetime = event.value.datetime,
        timezone = event.value.timezone,
        locked = event.value.locked,
        allowDonations = event.value.allowDonations,
        canonicalUrl = event.value.canonicalUrl,
        public = event.value.public,
        amount = event.value.amount,
        count = event.value.count,
        max = event.value.max,
        avg = event.value.avg,
    )
}
