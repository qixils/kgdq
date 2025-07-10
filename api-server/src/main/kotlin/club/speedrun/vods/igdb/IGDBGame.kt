package club.speedrun.vods.igdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IGDBGame(
//    val id: Long,
    val artworks: List<IGDBArtwork> = listOf(),
    val cover: IGDBCover? = null,
    @SerialName("first_release_date")
    val firstReleaseDate: Int = Int.MAX_VALUE,
    val name: String = "",
)
