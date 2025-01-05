package club.speedrun.vods.marathon.db

import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.Serializable

@Serializable
class BaseEvent(
    override val id: String,
    val short: String,
    val name: String,
    override val startsAt: InstantAsString,
    val donationAmount: Double? = null,
    val donationCount: Int? = null,
    override val cachedAt: InstantAsString,
) : TimedObject