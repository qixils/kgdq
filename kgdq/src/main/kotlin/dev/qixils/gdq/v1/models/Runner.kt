package dev.qixils.gdq.v1.models

import dev.qixils.gdq.computeStreamUrl
import kotlinx.serialization.Serializable

@Deprecated(message = "Use v2 API")
@Serializable
data class Runner(
    val name: String,
    val stream: String,
    val twitter: String,
    val youtube: String,
    val platform: String? = "TWITCH",
    val pronouns: String = "",
//    @SerialName("donor") private val _donor: Int? = null,
    val public: String,
) : AbstractModel() {
    val url: String? = computeStreamUrl(stream, youtube, twitter)
}