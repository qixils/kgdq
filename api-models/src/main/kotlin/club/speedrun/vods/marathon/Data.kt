@file:OptIn(ExperimentalSerializationApi::class)

package club.speedrun.vods.marathon

import club.speedrun.vods.naturalJoinToString
import dev.qixils.gdq.InternalGdqApi
import dev.qixils.gdq.models.Bid
import dev.qixils.gdq.models.BidState
import dev.qixils.gdq.models.Event
import dev.qixils.gdq.models.Headset
import dev.qixils.gdq.models.Run
import dev.qixils.gdq.models.Runner
import dev.qixils.gdq.models.Wrapper
import dev.qixils.gdq.serializers.DurationAsStringSerializer
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdSerializer
import dev.qixils.horaro.Horaro
import dev.qixils.horaro.models.FullSchedule
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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
    var gdqId: Int? = null
    var horaroId: String? = null
    // TODO: export RunOverrides ID for maps/sets/etc
    val event: Int
    val name: String
    val displayName: String
    val twitchName: String
    val console: String
    val commentators: MutableList<Headset>
    val hosts: MutableList<Headset>
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
    val vods: MutableList<VOD>
    var src: String? = null
    /**
     * The current status of the run in the schedule.
     */
    val timeStatus: TimeStatus

    constructor(
        run: Wrapper<Run>,
        bids: List<BidData>,
        previousRun: RunData?,
        overrides: RunOverrides,
    ) {
        trackerSource = run.value
        horaroSource = null
        gdqId = run.id
        horaroId = run.value.horaroId
        event = run.value.eventId
        name = run.value.name
        displayName = run.value.displayName
        twitchName = run.value.twitchName
        console = run.value.console
        commentators = mutableListOf()
        hosts = mutableListOf()
        description = run.value.description
        order = run.value.order
        coop = run.value.coop
        category = run.value.category
        releaseYear = run.value.releaseYear
        runners = mutableListOf()
        runnersAsString = run.value.deprecatedRunners.split(", ").naturalJoinToString()
        this.bids = bids.toMutableList()
        vods = overrides.vods.toMutableList()
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
        timeStatus = run {
            val now = Instant.now()
            when {
                now < startTime.minus(setupTime) -> TimeStatus.UPCOMING
                now < endTime -> TimeStatus.IN_PROGRESS
                else -> TimeStatus.FINISHED
            }
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
        gdqId = trackerRun?.gdqId
        horaroId = horaroRun.getValue("ID")
        this.event = event.id
        name = calculateHoraroName(horaroRun)
        displayName = trackerRun?.displayName ?: ""
        twitchName = trackerRun?.twitchName ?: horaroRun.getValue("Game (Twitch)") ?: "" // this could be stored from RabbitMQ, but I can't be bothered
        console = trackerRun?.console ?: horaroRun.getValue("Platform") ?: horaroRun.getValue("Console") ?: ""
        commentators = trackerRun?.commentators ?: mutableListOf()
        hosts = trackerRun?.hosts ?: mutableListOf()
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
        vods = calculateHoraroVODs(horaroRun) ?: overrides?.vods?.toMutableList() ?: mutableListOf()
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
        timeStatus = run {
            val now = Instant.now()
            when {
                now < startTime.minus(setupTime) -> TimeStatus.UPCOMING
                now < endTime -> TimeStatus.IN_PROGRESS
                else -> TimeStatus.FINISHED
            }
        }
    }

    @InternalGdqApi
    suspend fun loadData() {
        if (horaroSource != null) {
            if (runners.isNotEmpty())
                return
            Logger.getLogger("RunData").fine("Loading runners for $name")
            // The Horaro schedule has a "UserIDs" column, but they don't seem to align with the
            // donation tracker API, so I'm falling back to their "Player(s)" column instead
            runners.addAll((horaroSource!!.getValue("Player(s)") ?: horaroSource!!.getValue("Runners"))
                ?.split(", ")
                ?.map { calculateHoraroFakeRunner(it) } ?: emptyList())
        } else {
            runners.addAll(trackerSource!!.runners().map { it.value })
            commentators.addAll(trackerSource!!.commentators().map { it.value })
            hosts.addAll(trackerSource!!.hosts().map { it.value })
        }
    }

    val runTimeText: String get() = DurationAsStringSerializer.format(runTime)
    val setupTimeText: String get() = DurationAsStringSerializer.format(setupTime)

    /**
     * Whether this is the current run being played at the event.
     */
    val isCurrent: Boolean get() = timeStatus == TimeStatus.IN_PROGRESS

    companion object {
        private val MARKDOWN_LINK: Pattern = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)")
        private val MAX_RAW_SETUP_TIME = Duration.ofMinutes(30)

        fun calculateHoraroName(run: dev.qixils.horaro.models.Run): String {
            val rawName = run.getValue("Game")?.trim() ?: return "[Unknown Game]"
            // TODO: incorporate an actual markdown parser here to strip the Game field
            //  of markdown formatting (namely links)
            val matcher = MARKDOWN_LINK.matcher(rawName)
            val names = mutableListOf<String>()
            while (matcher.find())
                names += matcher.group(1)
            if (names.isEmpty())
                return rawName
            return names.naturalJoinToString()
        }

        private fun calculateHoraroVODs(run: dev.qixils.horaro.models.Run): MutableList<VOD>? {
            val rawName = run.getValue("Game")?.trim() ?: return null
            val matcher = MARKDOWN_LINK.matcher(rawName)
            val vods = mutableListOf<VOD>()
            while (matcher.find())
                VOD.fromUrlOrNull(matcher.group(2))?.let { vods += it }
            return if (vods.isEmpty()) null else vods
        }

        private fun calculateHoraroRunnerNames(run: dev.qixils.horaro.models.Run): List<String>? {
            val names = (run.getValue("Player(s)") ?: run.getValue("Runners"))?.split(", ") ?: return null
            return names.map {
                val matcher = MARKDOWN_LINK.matcher(it)
                if (matcher.matches()) {
                    matcher.group(1)
                } else {
                    it
                }
            }
        }

        private fun calculateHoraroFakeRunner(rawName: String): Runner {
            val matcher = MARKDOWN_LINK.matcher(rawName)
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
    val id: Int,
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
    constructor(bid: Wrapper<Bid>, children: List<BidData>, run: Wrapper<Run>) : this(
        id = bid.id,
        children = children,
        name = bid.value.name,
        state = when {
            bid.value.state != null -> bid.value.state!!
            run.value.endTime.isBefore(Instant.now()) -> BidState.CLOSED
            else -> BidState.OPENED
        },
        description = bid.value.description,
        shortDescription = bid.value.shortDescription,
        goal = bid.value.goal,
        isTarget = bid.value.isTarget,
        allowUserOptions = bid.value.allowUserOptions,
        optionMaxLength = bid.value.optionMaxLength,
        revealedAt = bid.value.revealedAt,
        donationTotal = bid.value.total,
        donationCount = bid.value.count,
        pinned = bid.value.pinned
    )
}

@Serializable
class EventData {
    val id: Int
    val short: String
    val name: String
    val hashtag: String
    val charityName: String
    val targetAmount: Float
    val minimumDonation: Float
    val paypalCurrency: String
    @Serializable(with = InstantAsStringSerializer::class) val startTime: Instant?
    @Serializable(with = InstantAsStringSerializer::class) val endTime: Instant?
    val timeStatus: TimeStatus?
    @Serializable(with = ZoneIdSerializer::class) val timezone: ZoneId
    val locked: Boolean
    val allowDonations: Boolean
    val canonicalUrl: String
    val public: String
    val amount: Double
    val count: Int
    val max: Double
    val avg: Double
    var horaroEvent: String? = null
    var horaroSchedule: String? = null
    val donationUrl: String
    val scheduleUrl: String

    constructor(organization: OrganizationConfig, event: Wrapper<Event>) {
        id = event.id
        short = event.value.short
        name = event.value.name
        hashtag = event.value.hashtag
        charityName = event.value.charityName
        targetAmount = event.value.targetAmount
        minimumDonation = event.value.minimumDonation
        paypalCurrency = event.value.paypalCurrency
        startTime = event.value.startTime
        endTime = event.value.endTime
        timezone = event.value.timezone
        locked = event.value.locked
        allowDonations = event.value.allowDonations
        canonicalUrl = event.value.canonicalUrl
        public = event.value.public
        amount = event.value.amount
        count = event.value.count
        max = event.value.max
        avg = event.value.avg
        horaroEvent = event.value.horaroEvent
        horaroSchedule = event.value.horaroSchedule

        val now = Instant.now()
        timeStatus = when {
            startTime == null -> null
            now < startTime -> TimeStatus.UPCOMING
            endTime == null -> TimeStatus.FINISHED
            now < endTime -> TimeStatus.IN_PROGRESS
            else -> TimeStatus.FINISHED
        }

        donationUrl = organization.getDonationUrl(this)
        scheduleUrl = organization.getScheduleUrl(this)
    }

    suspend fun horaroSchedule(): FullSchedule? {
        if (horaroEvent == null || horaroSchedule == null) return null
        return Horaro.getSchedule(horaroEvent!!, horaroSchedule!!)
    }

    val horaroUrl get() = "https://horaro.org/$horaroEvent/$horaroSchedule"
}

enum class TimeStatus {
    UPCOMING,
    IN_PROGRESS,
    FINISHED,
}

interface OrganizationConfig {
    /**
     * The identifier of this organization.
     */
    val id: String
    /**
     * The short name of this organization.
     */
    val shortName: String
    /**
     * The display name of this organization.
     */
    val displayName: String
    /**
     * The homepage URL of this organization.
     */
    val homepageUrl: String
    /**
     * Whether this organization supports automatic VOD link generation.
     */
    val autoVODs: Boolean
    /**
     * Creates the URL of the donation page for a given event.
     */
    fun getDonationUrl(event: EventData): String
    /**
     * Creates the URL of the schedule page for a given event.
     */
    fun getScheduleUrl(event: EventData): String
}

@Serializable
class OrganizationData {
    val displayName: String
    val shortName: String
    val homepageUrl: String
    val autoVODs: Boolean
    @EncodeDefault(EncodeDefault.Mode.NEVER) var amountRaised: Double? = null
    @EncodeDefault(EncodeDefault.Mode.NEVER) var donationCount: Int? = null

    constructor(organization: OrganizationConfig, events: List<EventData>?) {
        displayName = organization.displayName
        shortName = organization.shortName
        homepageUrl = organization.homepageUrl
        autoVODs = organization.autoVODs
        if (events != null) {
            amountRaised = events.sumOf { it.amount }
            donationCount = events.sumOf { it.count }
        }
    }
}
