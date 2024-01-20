package dev.qixils.gdq.v2.models

import kotlinx.serialization.Serializable

@Serializable
class Headset(
    override val type: String,
    override val id: Int,
    val name: String,
    val pronouns: String,
) : TypedModel()