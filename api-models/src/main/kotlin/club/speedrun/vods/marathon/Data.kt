package club.speedrun.vods.marathon

import club.speedrun.vods.naturalJoinToString
import dev.qixils.gdq.InternalGdqApi
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
    constructor(
        run: Wrapper<Run>,
        bids: List<BidData>,
        previousRun: RunData?,
        overrides: RunOverrides,
    ) : this(
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
        startTime = overrides.startTime ?: run.value.startTime,
        endTime = (overrides.startTime ?: run.value.startTime) + (overrides.runTime ?: run.value.runTime),
        order = run.value.order,
        runTime = overrides.runTime ?: run.value.runTime,
        setupTime = if (previousRun != null) {
            var duration = Duration.between(
                previousRun.endTime,
                overrides.startTime ?: run.value.startTime
            )
            if (duration.isNegative)
                duration = Duration.ZERO
            duration
        } else run.value.setupTime,
        coop = run.value.coop,
        category = run.value.category,
        releaseYear = run.value.releaseYear,
        runners = mutableListOf(),
        runnersAsString = run.value.deprecatedRunners.split(", ").naturalJoinToString(),
        bids = bids,
        twitchVODs = overrides.twitchVODs,
        youtubeVODs = overrides.youtubeVODs,
    )

    constructor(
        horaroRun: dev.qixils.horaro.models.Run,
        trackerRun: RunData?,
        previousRun: RunData?,
        event: Wrapper<Event>,
        order: Int,
        overrides: RunOverrides?,
    ) : this(
        trackerSource = trackerRun?.trackerSource,
        horaroSource = horaroRun,
        id = trackerRun?.id ?: -1,
        event = event.id,
        name = calculateHoraroName(horaroRun),
        displayName = trackerRun?.displayName ?: "",
        twitchName = trackerRun?.twitchName ?: "",
        console = trackerRun?.console ?: horaroRun.getValue("Platform") ?: "",
        commentators = trackerRun?.commentators ?: "",
        description = trackerRun?.description ?: "",
        startTime = overrides?.startTime ?: horaroRun.scheduled.toInstant(),
        endTime = (overrides?.startTime ?: horaroRun.scheduled.toInstant()).plus(overrides?.runTime ?: horaroRun.length),
        order = order,
        runTime = overrides?.runTime ?: horaroRun.length,
        setupTime = if (previousRun != null) {
            var duration = Duration.between(
                previousRun.endTime,
                overrides?.startTime ?: horaroRun.scheduled.toInstant()
            )
            if (duration.isNegative)
                duration = Duration.ZERO
            duration
        } else trackerRun?.setupTime ?: Duration.ZERO,
        coop = trackerRun?.coop ?: false,
        category = trackerRun?.category ?: horaroRun.getValue("Category") ?: "",
        releaseYear = trackerRun?.releaseYear,
        runners = trackerRun?.runners ?: mutableListOf(),
        runnersAsString = listOf(
            calculateHoraroRunnerNames(horaroRun)?.naturalJoinToString(),
            trackerRun?.runnersAsString
        ).firstOrNull { !it.isNullOrEmpty() } ?: "",
        bids = trackerRun?.bids ?: emptyList(),
        twitchVODs = calculateHoraroVODs(horaroRun) ?: overrides?.twitchVODs ?: emptyList(),
        youtubeVODs = trackerRun?.youtubeVODs ?: overrides?.youtubeVODs ?: emptyList(),
    )

    @InternalGdqApi
    suspend fun loadRunners() {
        if (horaroSource != null) {
            if (runners.isNotEmpty())
                return
            Logger.getLogger("RunData").fine("Loading runners for $name")
            // The Horaro schedule has a "UserIDs" column, but they don't seem to align with the
            // donation tracker API, so I'm falling back to their "Player(s)" column instead
            runners.addAll(horaroSource.getValue("Player(s)")?.split(", ")
                ?.map { calculateHoraroFakeRunner(it) } ?: emptyList())
        } else {
            runners.addAll(trackerSource!!.runners().map { it.value })
        }
    }

    val runTimeText: String get() = DurationAsStringSerializer.format(runTime)
    val setupTimeText: String get() = DurationAsStringSerializer.format(setupTime)

    /**
     * Whether this is the current run being played at the event.
     */
    @Transient
    val isCurrent: Boolean = run {
        val now = Instant.now()
        val start = startTime.minus(setupTime)
        val end = endTime
        start <= now && now <= end
    }

    companion object {
        private val HORARO_GAME_MARKDOWN: Pattern =
            Pattern.compile("\\[([^\\[\\]]+)]\\(https?://(?:www.)?twitch.tv/videos/(\\d+)\\)")
        private val NAME_REGEX: Pattern = Pattern.compile("\\[(.+)]\\((.+)\\)")

        private fun calculateHoraroName(run: dev.qixils.horaro.models.Run): String {
            val rawName = run.getValue("Game")!!.trim()
            val matcher = HORARO_GAME_MARKDOWN.matcher(rawName)
            val names = mutableListOf<String>()
            while (matcher.find())
                names += matcher.group(1)
            if (names.isEmpty())
                return rawName
            return names.naturalJoinToString()
        }

        private fun calculateHoraroVODs(run: dev.qixils.horaro.models.Run): List<TwitchVOD>? {
            val rawName = run.getValue("Game")!!.trim()
            val matcher = HORARO_GAME_MARKDOWN.matcher(rawName)
            val vods = mutableListOf<TwitchVOD>()
            while (matcher.find())
                vods += TwitchVOD(matcher.group(2))
            return if (vods.isEmpty()) null else vods
        }

        private fun calculateHoraroRunnerNames(run: dev.qixils.horaro.models.Run): List<String>? {
            val names = run.getValue("Player(s)")?.split(", ") ?: return null
            return names.map {
                val matcher = NAME_REGEX.matcher(it)
                if (matcher.matches()) {
                    matcher.group(1)
                } else {
                    it
                }
            }
        }

        private fun calculateHoraroFakeRunner(rawName: String): Runner {
            val matcher = NAME_REGEX.matcher(rawName)
            val name: String
            val stream: String
            if (matcher.matches()) {
                name = matcher.group(1)
                stream = matcher.group(2)
            } else {
                name = rawName
                stream = ""
            }
            return Runner(name, stream, "", "", "", name)
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
    constructor(bid: Bid, children: List<BidData>, run: Wrapper<Run>) : this(
        children = children,
        name = bid.name,
        state = when {
            bid.state != null -> bid.state!!
            run.value.endTime.isBefore(Instant.now()) -> BidState.CLOSED
            else -> BidState.OPENED
        },
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
