package club.speedrun.vods

import club.speedrun.vods.igdb.IGDB
import club.speedrun.vods.marathon.*
import club.speedrun.vods.marathon.db.BaseBid
import club.speedrun.vods.marathon.db.BaseEvent
import club.speedrun.vods.marathon.db.BaseRun
import club.speedrun.vods.marathon.db.BaseTalent
import club.speedrun.vods.marathon.gdq.DonationTrackerDatabase
import dev.qixils.gdq.BidState
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern
import dev.qixils.horaro.models.Run as HoraroRun

private val MARKDOWN_LINK: Pattern = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)")
private val MAX_RAW_SETUP_TIME = Duration.ofMinutes(30)
val excludedGameTitles = listOf("bonus game", "daily recap", "tasbot plays")

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

private fun calculateHoraroFakeRunner(rawName: String): TalentData {
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
    return TalentData(name, stream)
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

fun createTalent(runner: BaseTalent)
= TalentData(runner.name, runner.url, pronouns = runner.pronouns)

suspend fun createRun(
    run: BaseRun,
    previousRun: RunData?,
    overrides: RunOverrides,
    db: DonationTrackerDatabase,
): RunData {
    val startTime = overrides.startTime
        ?: previousRun?.endTime?.let { it + run.setupTime }
        ?: run.startsAt
    val setupTime = if (previousRun != null) {
        var duration = Duration.between(previousRun.endTime, startTime)
        if (duration.isNegative)
            duration = Duration.ZERO
        duration
    } else {
        run.setupTime
    }
    val runTime = overrides.runTime ?: run.runTime
    val endTime = startTime + runTime

    val gameName = when {
        !run.twitchGame.isNullOrEmpty() -> run.twitchGame
        !run.displayGame.isNullOrEmpty() -> run.displayGame
        else -> run.game
    }
    val isGame = excludedGameTitles.none { gameName.contains(it, true) }

    val igdb = if (isGame) IGDB.getCached(gameName)?.result?.let { res -> IGDBData(
        background = res.artworks.sortedWith { a, b ->
            if (a.artworkType != b.artworkType) {
                if (a.artworkType == 2) return@sortedWith -1
                if (b.artworkType == 2) return@sortedWith 1
                if (a.artworkType == 1) return@sortedWith -1
                if (b.artworkType == 1) return@sortedWith 1
            }
            if (a.animated != b.animated) return@sortedWith if (a.animated) 1 else -1
            if (a.alphaChannel != b.alphaChannel) return@sortedWith if (a.alphaChannel) 1 else -1
            return@sortedWith 0
        }.firstOrNull()?.imageId,
        cover = res.cover?.imageId,
    ) }
    else null

    return RunData(
        id = run.id,
        name = run.game,
        twitchName = run.twitchGame,
        console = run.console,
        runners = run.runners.mapNotNull { db.talent.getByIdForce(it) }.map { createTalent(it.obj) },
        commentators = run.commentators.mapNotNull { db.talent.getByIdForce(it) }.map { createTalent(it.obj) },
        hosts = run.hosts.mapNotNull { db.talent.getByIdForce(it) }.map { createTalent(it.obj) },
        description = run.description,
        coop = run.coop,
        category = run.category,
        releaseYear = run.releaseYear,
        bids = run.bids.map { createBid(it, run) },
        vods = overrides.vods.toMutableList(),
        startTime = startTime,
        endTime = endTime,
        runTime = runTime,
        setupTime = setupTime,
        src = when {
            overrides.src == "" -> null
            overrides.src != null -> overrides.src
            isGame -> srcDb.getGame(gameName).abbreviation
            else -> null
        },
        igdb = igdb,
    )
}

fun createBid(bid: BaseBid, run: BaseRun): BidData {
    return BidData(
        id = bid.id,
        children = bid.children.map { createBid(it, run) },
        name = bid.name,
        state = when {
            bid.open -> BidState.OPENED
            run.startsAt.plus(run.runTime).isBefore(Instant.now()) -> BidState.CLOSED
            else -> BidState.OPENED
        },
        description = bid.description,
        shortDescription = bid.shortDescription,
        goal = bid.goal,
        isTarget = bid.isTarget,
        allowUserOptions = bid.allowsUserOptions,
        optionMaxLength = bid.optionMaxLength,
        donationTotal = bid.donationTotal,
        donationCount = bid.donationCount,
    )
}

fun createEvent(organization: OrganizationConfig, event: BaseEvent, runs: List<RunData>?): EventData {
    return EventData(
        id = event.id,
        short = event.short,
        name = event.name,
        startTime = event.startsAt,
        timezone = event.timezone,
        endTime = runs?.lastOrNull()?.endTime,
        amount = event.donationAmount ?: 0.0,
        count = event.donationCount,
        charityName = event.charityName,
        currency = event.currency,
        donationUrl = organization.getDonationUrl(event),
        scheduleUrl = organization.getScheduleUrl(event),
    )
}
