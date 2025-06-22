package club.speedrun.vods.marathon.db

import dev.qixils.gdq.serializers.InstantAsString
import dev.qixils.gdq.serializers.ZoneIdAsString
import kotlinx.serialization.Serializable

@Serializable
class BaseEvent(
    override val id: String,
    val short: String,
    val name: String,
    override val startsAt: InstantAsString,
    val timezone: ZoneIdAsString,
    val donationAmount: Double? = null,
    val donationCount: Int? = null,
    val charityName: String? = null,
    val charityShort: String? = null,
    val currency: String? = null,
    override val cachedAt: InstantAsString,
) : TimedObject