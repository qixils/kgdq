package club.speedrun.vods.marathon.db

import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.Serializable

@Serializable
class BaseHeadset(
    override val id: String,
    val name: String = "Commentator",
    val pronouns: String? = null,
    override val cachedAt: InstantAsString,
) : IObject