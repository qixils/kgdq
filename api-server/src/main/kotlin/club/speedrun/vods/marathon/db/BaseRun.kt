package club.speedrun.vods.marathon.db

import club.speedrun.vods.marathon.VOD
import dev.qixils.gdq.serializers.DurationAsString
import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.Serializable
import java.time.Duration

@Serializable
class BaseRun(
    override val id: String,
    val event: String,
    val game: String = "Unknown Game",
    val displayGame: String? = null,
    val twitchGame: String? = null,
    val description: String? = null,
    val category: String? = null,
    val console: String? = null,
    val runners: List<String> = emptyList(), // TODO: these may need to become some sort of wrapper class for non-GDQ orgs
    val hosts: List<String> = emptyList(),
    val commentators: List<String> = emptyList(),
    override val startsAt: InstantAsString,
    val runTime: DurationAsString = Duration.ZERO,
    val setupTime: DurationAsString = Duration.ZERO,
    val bids: List<BaseBid> = emptyList(),
    val coop: Boolean = false,
    val releaseYear: Int? = null,
    val vods: List<VOD> = emptyList(),
    override val cachedAt: InstantAsString,
) : TimedObject