package club.speedrun.vods.marathon.db

import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.Serializable

@Serializable
class BaseTalent(
    override val id: String,
    val name: String = "Runner",
    val pronouns: String? = null,
    val url: String? = null,
    override val cachedAt: InstantAsString,
) : IObject