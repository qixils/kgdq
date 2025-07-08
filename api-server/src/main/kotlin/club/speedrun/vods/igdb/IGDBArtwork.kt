package club.speedrun.vods.igdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IGDBArtwork(
//    val id: Long,
    @SerialName("image_id")
    val imageId: String,
    @SerialName("artwork_type")
    val artworkType: Int,
    @SerialName("alpha_channel")
    val alphaChannel: Boolean = false,
    @SerialName("animated")
    val animated: Boolean = false,
)
