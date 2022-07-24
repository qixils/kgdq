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
//    val donor: Int?,
    val public: String
) : Model
