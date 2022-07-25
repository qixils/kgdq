package dev.qixils.gdq.models

import kotlinx.serialization.Serializable

@Serializable
data class Runner(
    val name: String,
    val stream: String,
    val twitter: String,
    val youtube: String,
//    val platform: String?, - this field is always set to "TWITCH" and thus misleading & useless
    val pronouns: String = "",
//    @SerialName("donor") private val _donor: Int? = null,
    val public: String,
) : Model
