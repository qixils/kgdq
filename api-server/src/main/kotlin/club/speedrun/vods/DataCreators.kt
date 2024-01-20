package club.speedrun.vods

import club.speedrun.vods.marathon.*
import dev.qixils.gdq.v1.models.*
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern
import dev.qixils.gdq.v2.models.Headset as HeadsetV2
import dev.qixils.gdq.v2.models.Runner as RunnerV2
import dev.qixils.horaro.models.Run as HoraroRun

private val MARKDOWN_LINK: Pattern = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)")
private val MAX_RAW_SETUP_TIME = Duration.ofMinutes(30)

fun calculateHoraroName(run: HoraroRun): String {
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

private fun calculateHoraroVODs(run: HoraroRun): MutableList<VOD>? {
    val rawName = run.getValue("Game")?.trim() ?: return null
    val matcher = MARKDOWN_LINK.matcher(rawName)
    val vods = mutableListOf<VOD>()
    while (matcher.find())
        VOD.fromUrlOrNull(matcher.group(2))?.let { vods += it }
    return if (vods.isEmpty()) null else vods
}

private fun calculateHoraroRunnerNames(run: HoraroRun): List<String>? {
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

private fun calculateHoraroFakeRunner(rawName: String): RunnerData {
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
    return RunnerData(name, stream)
}

private fun calculateHoraroRawSetupTime(run: HoraroRun, previousRun: HoraroRun?): Duration? {
    if (previousRun == null)
        return null
    val previousEnd = previousRun.scheduled + previousRun.length
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

fun createRunner(runner: Runner)
= RunnerData(runner.name, runner.url, runner.twitter, runner.youtube, runner.pronouns)

fun createRunner(runner: RunnerV2)
= RunnerData(runner.name, runner.url, runner.twitter, runner.youtube, runner.pronouns)

fun createHeadset(headset: Headset)
= HeadsetData(headset.name, headset.pronouns)

fun createHeadset(headset: HeadsetV2)
= HeadsetData(headset.name, headset.pronouns)

suspend fun createRun(
    run: Run,
    bids: List<BidData>,
    previousRun: RunData?,
    overrides: RunOverrides,
): RunData {
    // TODO: reduce code duplication
    val startTime = overrides.startTime?.let { it - run.setupTime }
        ?: previousRun?.endTime
        ?: run.startTime
    val setupTime = if (previousRun != null) {
        var duration = Duration.between(previousRun.endTime, startTime + run.setupTime)
        if (duration.isNegative)
            duration = Duration.ZERO
        duration
    } else {
        run.setupTime
    }
    val runTime = overrides.runTime ?: run.runTime
    val endTime = startTime + runTime + setupTime

    return RunData(
        gdqId = run.id,
        horaroId = run.horaroId,
        name = run.name,
        displayName = run.displayName,
        twitchName = run.twitchName,
        console = run.console,
        runners = run.fetchRunners().map { createRunner(it) },
        commentators = run.fetchCommentators().map { createHeadset(it) },
        hosts = run.fetchHosts().map { createHeadset(it) },
        description = run.description,
        coop = run.coop,
        category = run.category,
        releaseYear = run.releaseYear,
        bids = bids,
        vods = overrides.vods,
        startTime = startTime,
        endTime = endTime,
        runTime = runTime,
        setupTime = setupTime,
    )
}

fun createRun(
    horaroRun: HoraroRun,
    trackerRun: RunData?,
    previousHoraroRun: HoraroRun?,
    previousRun: RunData?,
    event: Event,
    order: Int,
    overrides: RunOverrides?,
): RunData {
    val startTime = overrides?.startTime
        ?: calculateOffsetTime(
            previousRun?.endTime,
            calculateHoraroRawSetupTime(horaroRun, previousHoraroRun)
        )
        ?: horaroRun.scheduled.toInstant()
    val setupTime = if (previousRun != null) {
        var duration = Duration.between(previousRun.endTime, startTime)
        if (duration.isNegative)
            duration = Duration.ZERO
        duration
    } else {
        trackerRun?.setupTime ?: Duration.ZERO
    }
    val runTime = overrides?.runTime ?: horaroRun.length
    val endTime = startTime + runTime

    return RunData(
        gdqId = trackerRun?.gdqId,
        horaroId = horaroRun.getValue("ID"),
        name = calculateHoraroName(horaroRun),
        displayName = trackerRun?.displayName ?: "",
        twitchName = trackerRun?.twitchName ?: horaroRun.getValue("Game (Twitch)") ?: "", // this could be stored from RabbitMQ, but I can't be bothered
        console = trackerRun?.console ?: horaroRun.getValue("Platform") ?: horaroRun.getValue("Console") ?: "",
        commentators = trackerRun?.commentators ?: emptyList(),
        hosts = trackerRun?.hosts ?: emptyList(),
        description = trackerRun?.description ?: "",
        coop = trackerRun?.coop ?: false,
        category = trackerRun?.category ?: horaroRun.getValue("Category") ?: "",
        releaseYear = trackerRun?.releaseYear,
        runners = trackerRun?.runners
            ?: (horaroRun.getValue("Player(s)") ?: horaroRun.getValue("Runners"))
                ?.split(", ")
                ?.map { calculateHoraroFakeRunner(it) }
            ?: emptyList(),
        bids = trackerRun?.bids ?: emptyList(),
        vods = calculateHoraroVODs(horaroRun) ?: overrides?.vods?.toMutableList() ?: emptyList(),
        startTime = startTime,
        endTime = endTime,
        runTime = runTime,
        setupTime = setupTime,
    )
}

fun createBid(bid: Bid, children: List<BidData>, run: Run) : BidData {
    return BidData(
        id = bid.id,
        children = children,
        name = bid.name,
        state = when {
            bid.state != null -> bid.state!!
            run.endTime.isBefore(Instant.now()) -> BidState.CLOSED
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

fun createEvent(organization: OrganizationConfig, event: Event): EventData {
    return EventData(
        id = event.id,
        short = event.short,
        name = event.name,
        hashtag = event.hashtag,
        charityName = event.charityName,
        targetAmount = event.targetAmount,
        minimumDonation = event.minimumDonation,
        paypalCurrency = event.paypalCurrency,
        startTime = event.startTime,
        endTime = event.endTime,
        timezone = event.timezone,
        locked = event.locked,
        allowDonations = event.allowDonations,
        canonicalUrl = event.canonicalUrl,
        public = event.public,
        amount = event.amount,
        count = event.count,
        max = event.max,
        avg = event.avg,
        horaroEvent = event.horaroEvent,
        horaroSchedule = event.horaroSchedule,
    )
}
