package dev.qixils.gdq.v2.models

import kotlinx.serialization.Serializable

@Serializable
class Runner(
    override val type: String,
    override val id: Int,
    val name: String = "",
    val pronouns: String = "",
    val stream: String = "",
    val twitter: String = "",
    val youtube: String = "",
    val platform: String = "TWITCH",
) : TypedModel()