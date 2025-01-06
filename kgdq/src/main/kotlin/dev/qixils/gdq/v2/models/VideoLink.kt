package dev.qixils.gdq.v2.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VideoLink(
    override val id: Int,
    @SerialName("link_type") val linkType: String,
    val url: String,
) : IdentifiedModel()