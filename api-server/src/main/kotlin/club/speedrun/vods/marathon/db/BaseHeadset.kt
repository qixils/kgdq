package club.speedrun.vods.marathon.db

import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.Serializable

@Serializable
@Deprecated(replaceWith = ReplaceWith("Talent", "club.speedrun.vods.marathon.db.BaseTalent"), message = "merged with BaseRunner", level = DeprecationLevel.ERROR)
class BaseHeadset(
    override val id: String,
    val name: String = "Commentator",
    val pronouns: String? = null,
    override val cachedAt: InstantAsString,
) : IObject