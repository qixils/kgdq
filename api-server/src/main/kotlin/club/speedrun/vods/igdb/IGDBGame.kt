package club.speedrun.vods.igdb

import kotlinx.serialization.Serializable

@Serializable
data class IGDBGame(
//    val id: Long,
    val artworks: List<IGDBArtwork>,
    val cover: IGDBCover? = null,
)
