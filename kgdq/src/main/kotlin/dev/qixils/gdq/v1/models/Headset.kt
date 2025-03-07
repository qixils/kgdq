package dev.qixils.gdq.v1.models

import kotlinx.serialization.Serializable

/**
 * A person wearing a headset.
 * <p>
 * Yes, this is GDQ's real name for this class.
 * No, the runner class does not directly extend it.
 */
@Deprecated(message = "Use v2 API")
@Serializable
data class Headset(
    val name: String,
    val pronouns: String = "",
    // val runner: Int, | TODO: not sure yet of this type
    val public: String = if (pronouns.isEmpty()) name else "$name ($pronouns)",
) : AbstractModel()