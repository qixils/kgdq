package dev.qixils.gdq.v1.models

import dev.qixils.gdq.v1.GDQ
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A person wearing a headset.
 * <p>
 * Yes, this is GDQ's real name for this class.
 * No, the runner class does not directly extend it.
 */
@Serializable
data class Headset(
    val name: String,
    val pronouns: String = "",
    // val runner: Int, | TODO: not sure yet of this type
    val public: String = if (pronouns.isEmpty()) name else "$name ($pronouns)",
) : Model {
    @Transient override var api: GDQ? = null
    override var id: Int? = null
}