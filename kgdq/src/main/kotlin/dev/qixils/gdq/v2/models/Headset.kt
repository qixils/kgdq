package dev.qixils.gdq.v2.models

import kotlinx.serialization.Serializable

@Serializable
@Deprecated(replaceWith = ReplaceWith("Talent", "dev.qixils.gdq.v2.models.Talent"), message = "merged with Runner", level = DeprecationLevel.ERROR)
class Headset(
    override val type: String,
    override val id: Int,
    val name: String,
    val pronouns: String,
) : TypedModel()