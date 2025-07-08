package club.speedrun.vods.igdb

import club.speedrun.vods.marathon.db.IObject
import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.Serializable

@Serializable
data class IGDBGameSearch(
    /**
     * Filesystem-friendly unique ID based on a hash of the search query.
     */
    override val id: String,
    /**
     * The original search query.
     */
    val search: String,
    val result: IGDBGame?,
    override val cachedAt: InstantAsString,
) : IObject