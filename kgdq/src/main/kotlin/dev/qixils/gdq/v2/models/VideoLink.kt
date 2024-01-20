package dev.qixils.gdq.v2.models

import kotlinx.serialization.SerialName

class VideoLink(
    val id: Int,
    @SerialName("link_type") val linkType: String,
    val url: String,
) : Model()