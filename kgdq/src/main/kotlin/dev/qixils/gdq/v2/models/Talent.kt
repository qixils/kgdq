package dev.qixils.gdq.v2.models

import dev.qixils.gdq.computeStreamUrl
import kotlinx.serialization.Serializable

@Serializable
data class Talent(
    override val type: String,
    override val id: Int,
    val name: String = "",
    val pronouns: String = "",
    val stream: String = "",
    val twitter: String = "",
    val youtube: String = "",
    val platform: String = "TWITCH",
) : TypedModel() {
    val url: String? = computeStreamUrl(stream, youtube, twitter)
}