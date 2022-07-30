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
class RunData{
    @Transient var trackerSource: Run? = null
    @Transient var horaroSource: dev.qixils.horaro.models.Run? = null
    var id: Int? = null
    var horaroId: String? = null
    val event: Int
    val name: String
    val displayName: String
    val twitchName: String
    val console: String
    val commentators: String
    val description: String
    @Serializable(with = InstantAsStringSerializer::class) val startTime: Instant
    @Serializable(with = InstantAsStringSerializer::class) val endTime: Instant
    val order: Int
    @Serializable(with = DurationAsStringSerializer::class) val runTime: Duration
    @Serializable(with = DurationAsStringSerializer::class) val setupTime: Duration
    val coop: Boolean
    val category: String
    var releaseYear: Int? = null
    val runners: MutableList<Runner>
    val runnersAsString: String
    val bids: MutableList<BidData>
    val twitchVODs: MutableList<TwitchVOD>
    val youtubeVODs: MutableList<YouTubeVOD>

    constructor(
        run: Wrapper<Run>,
        bids: List<BidData>,
        previousRun: RunData?,
        overrides: RunOverrides,
    ) {
        trackerSource = run.value
        horaroSource = null
        id = run.id
        horaroId = run.value.horaroId
        event = run.value.eventId
        name = run.value.name
        displayName = run.value.displayName
        twitchName = run.value.twitchName
        console = run.value.console
        commentators = run.value.commentators
        description = run.value.description
        order = run.value.order
        coop = run.value.coop
        category = run.value.category
        releaseYear = run.value.releaseYear
        runners = mutableListOf()
        runnersAsString = run.value.deprecatedRunners.split(", ").naturalJoinToString()
        this.bids = bids.toMutableList()
        twitchVODs = overrides.twitchVODs
        youtubeVODs = overrides.youtubeVODs
        startTime = overrides.startTime
            ?: calculateOffsetTime(previousRun?.endTime, run.value.setupTime)
            ?: run.value.startTime
        runTime = overrides.runTime ?: run.value.runTime
        endTime = startTime + runTime
        if (previousRun != null) {
            var duration = Duration.between(previousRun.endTime, startTime)
            if (duration.isNegative)
                duration = Duration.ZERO
            setupTime = duration
        } else {
            setupTime = run.value.setupTime
        }
    }

    constructor(
        horaroRun: dev.qixils.horaro.models.Run,
        trackerRun: RunData?,
        previousRun: RunData?,
        event: Wrapper<Event>,
        order: Int,
        overrides: RunOverrides?,
    ) {
        trackerSource = trackerRun?.trackerSource
        horaroSource = horaroRun
        id = trackerRun?.id
        horaroId = horaroRun.getValue("ID")
        this.event = event.id
        name = calculateHoraroName(horaroRun)
        displayName = trackerRun?.displayName ?: ""
        twitchName = trackerRun?.twitchName ?: "" // this could be stored from RabbitMQ, but I can't be bothered
        console = trackerRun?.console ?: horaroRun.getValue("Platform") ?: ""
        commentators = trackerRun?.commentators ?: ""
        description = trackerRun?.description ?: ""
        this.order = order
        coop = trackerRun?.coop ?: false
        category = trackerRun?.category ?: horaroRun.getValue("Category") ?: ""
        releaseYear = trackerRun?.releaseYear
        runners = trackerRun?.runners ?: mutableListOf()
        runnersAsString = listOf(
            calculateHoraroRunnerNames(horaroRun)?.naturalJoinToString(),
            trackerRun?.runnersAsString
        ).firstOrNull { !it.isNullOrEmpty() } ?: ""
        bids = trackerRun?.bids ?: mutableListOf()
        twitchVODs = calculateHoraroVODs(horaroRun) ?: overrides?.twitchVODs ?: mutableListOf()
        youtubeVODs = trackerRun?.youtubeVODs ?: overrides?.youtubeVODs ?: mutableListOf()
        startTime = overrides?.startTime
            ?: calculateOffsetTime(previousRun?.endTime, calculateHoraroRawSetupTime(horaroRun, previousRun))
            ?: horaroRun.scheduled.toInstant()
        runTime = overrides?.runTime ?: horaroRun.length
        endTime = startTime + runTime
        if (previousRun != null) {
            var duration = Duration.between(previousRun.endTime, startTime)
            if (duration.isNegative)
                duration = Duration.ZERO
            setupTime = duration
        } else {
            setupTime = trackerRun?.setupTime ?: Duration.ZERO
        }
    }

    @InternalGdqApi
    suspend fun loadRunners() {
        if (horaroSource != null) {
            if (runners.isNotEmpty())
                return
            Logger.getLogger("RunData").fine("Loading runners for $name")
            // The Horaro schedule has a "UserIDs" column, but they don't seem to align with the
            // donation tracker API, so I'm falling back to their "Player(s)" column instead
            runners.addAll(horaroSource!!.getValue("Player(s)")?.split(", ")
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
    val isCurrent: Boolean get() {
        val now = Instant.now()
        val start = startTime.minus(setupTime)
        val end = endTime
        return start <= now && now <= end
    }

    companion object {
        private val HORARO_GAME_MARKDOWN: Pattern =
            Pattern.compile("\\[([^\\[\\]]+)]\\(https?://(?:www.)?(?:twitch.tv/videos/(\\d+))?\\)")
        private val NAME_REGEX: Pattern = Pattern.compile("\\[(.+)]\\((.+)\\)")
        private val MAX_RAW_SETUP_TIME = Duration.ofMinutes(30)

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

        private fun calculateHoraroVODs(run: dev.qixils.horaro.models.Run): MutableList<TwitchVOD>? {
            val rawName = run.getValue("Game")!!.trim()
            val matcher = HORARO_GAME_MARKDOWN.matcher(rawName)
            val vods = mutableListOf<TwitchVOD>()
            while (matcher.find()) {
                val videoId = matcher.group(2)
                if (videoId != null)
                    vods += TwitchVOD(videoId)
            }
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

        private fun calculateHoraroRawSetupTime(run: dev.qixils.horaro.models.Run, previousRun: RunData?): Duration? {
            if (previousRun == null)
                return null
            val previousEnd = previousRun.horaroSource!!.scheduled + previousRun.horaroSource!!.length
            val currentStart = run.scheduled
            return Duration.between(previousEnd, currentStart)
        }

        private fun calculateOffsetTime(previousRunTime: Instant?, setupTime: Duration?): Instant? {
            if (previousRunTime == null)
                return null
            if (setupTime == null)
                return null
            // if the setup time is longer than half an hour then the run is probably scheduled for
            // the next day of programming, so we'd rather return null to fall back to its
            // officially scheduled start time.
            if (setupTime > MAX_RAW_SETUP_TIME)
                return null
            return previousRunTime + setupTime
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
