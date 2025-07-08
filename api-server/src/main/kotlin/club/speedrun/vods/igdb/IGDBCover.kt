package club.speedrun.vods.igdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IGDBCover(
//    val id: Long,
    @SerialName("image_id")
    val imageId: String,
)
