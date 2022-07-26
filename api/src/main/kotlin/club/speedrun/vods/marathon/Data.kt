package club.speedrun.vods.marathon

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.InternalGdqApi
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.models.*
import dev.qixils.gdq.serializers.DurationAsStringSerializer
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.logging.Logger
import java.util.regex.Pattern

@Serializable
data class RunData(
    @Transient val trackerSource: Run? = null,
    @Transient val horaroSource: dev.qixils.horaro.models.Run? = null,
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
    val runnersAsString: String,
    val bids: List<BidData>,
    val twitchVODs: List<TwitchVOD>,
    val youtubeVODs: List<YouTubeVOD>,
) {
    // TODO: add parameter for RunOverrides; class should have overrides for:
    //  - startTime (to be used by startTime, endTime, and setupTime)
    //  - runTime (to be used by runTime, endTime)
    //  - twitchVODs
    //  - youtubeVODs
    constructor(run: Wrapper<Run>, bids: List<BidData>, previousRun: RunData?) : this(
        trackerSource = run.value,
        horaroSource = null,
        id = run.id,
        event = run.value.eventId,
        name = run.value.name,
        displayName = run.value.displayName,
        twitchName = run.value.twitchName,
        console = run.value.console,
        commentators = run.value.commentators,
        description = run.value.description,
        startTime = run.value.startTime,
        endTime = run.value.startTime + run.value.runTime,
        order = run.value.order,
        runTime = run.value.runTime,
        setupTime = if (previousRun != null) Duration.between(previousRun.endTime, run.value.startTime) else run.value.setupTime,
        coop = run.value.coop,
        category = run.value.category,
        releaseYear = run.value.releaseYear,
        runners = mutableListOf(),
        runnersAsString = run.value.deprecatedRunners,
        bids = bids,
        twitchVODs = emptyList(),
        youtubeVODs = emptyList(),
    )

    constructor(horaroRun: dev.qixils.horaro.models.Run, trackerRun: RunData?, previousRun: RunData?, event: Wrapper<Event>, order: Int) : this(
        trackerSource = trackerRun?.trackerSource,
        horaroSource = horaroRun,
        id = trackerRun?.id ?: -1,
        event = event.id,
        name = trackerRun?.name ?: calculateHoraroName(horaroRun),
        displayName = trackerRun?.displayName ?: "",
        twitchName = trackerRun?.twitchName ?: "",
        console = trackerRun?.console ?: horaroRun.getValue("Platform") ?: "",
        commentators = trackerRun?.commentators ?: "",
        description = trackerRun?.description ?: "",
        startTime = horaroRun.scheduled.toInstant(),
        endTime = horaroRun.scheduled.toInstant().plus(horaroRun.length),
        order = order,
        runTime = horaroRun.length,
        setupTime = if (previousRun != null) Duration.between(previousRun.endTime, horaroRun.scheduled.toInstant()) else trackerRun?.setupTime ?: Duration.ZERO,
        coop = trackerRun?.coop ?: false,
        category = trackerRun?.category ?: horaroRun.getValue("Category") ?: "",
        releaseYear = trackerRun?.releaseYear,
        runners = trackerRun?.runners ?: mutableListOf(),
        runnersAsString = listOf(horaroRun.getValue("Player(s)"), trackerRun?.runnersAsString).firstOrNull { !it.isNullOrEmpty() } ?: "",
        bids = trackerRun?.bids ?: emptyList(),
        twitchVODs = calculateHoraroVODs(horaroRun) ?: trackerRun?.twitchVODs ?: emptyList(),
        youtubeVODs = trackerRun?.youtubeVODs ?: emptyList(),
    )

    @InternalGdqApi
    suspend fun loadRunners(api: GDQ) {
        if (horaroSource != null) {
            if (runners.isNotEmpty())
                return
            Logger.getLogger("RunData").fine("Loading runners for $name")
            val ids = horaroSource.getValue("UserIDs")?.split(",") ?: emptyList()
            runners.addAll(ids.map { api.query(type = ModelType.RUNNER, id = it.toInt()).first().value })
        } else {
            runners.addAll(trackerSource!!.runners().map { it.value })
        }
    }

    val runTimeText: String get() = DurationAsStringSerializer.format(runTime)
    val setupTimeText: String get() = DurationAsStringSerializer.format(setupTime)

    /**
     * Whether this is the current run being played at the event.
     */
    @Transient val isCurrent: Boolean = run {
        val now = Instant.now()
        val start = startTime.minus(setupTime)
        val end = endTime
        start <= now && now <= end
    }

    companion object {
        private val HORARO_GAME_MARKDOWN: Pattern = Pattern.compile("\\[(.+)]\\(https://www.twitch.tv/videos/(\\d+)\\)")

        private fun calculateHoraroName(run: dev.qixils.horaro.models.Run): String {
            val rawName = run.getValue("Game")!!.trim()
            val matcher = HORARO_GAME_MARKDOWN.matcher(rawName)
            return if (matcher.matches()) {
                matcher.group(1)
            } else {
                rawName
            }
        }

        private fun calculateHoraroVODs(run: dev.qixils.horaro.models.Run): List<TwitchVOD>? {
            val rawName = run.getValue("Game")!!.trim()
            val matcher = HORARO_GAME_MARKDOWN.matcher(rawName)
            return if (matcher.matches()) {
                listOf(TwitchVOD(matcher.group(2).toInt()))
            } else {
                null
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
