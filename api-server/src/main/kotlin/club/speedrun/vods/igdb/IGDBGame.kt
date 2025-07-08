package club.speedrun.vods.igdb

import kotlinx.serialization.Serializable

@Serializable
data class IGDBGame(
//    val id: Long,
    val artworks: List<IGDBArtwork> = listOf(),
    val cover: IGDBCover? = null,
)
