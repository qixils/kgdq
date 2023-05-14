package dev.qixils.gdq.models

import dev.qixils.gdq.serializers.AbstractLegacyStringAdapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A person wearing a headset.
 * <p>
 * Yes, this is GDQ's real name for this class.
 * No, the runner class does not directly extend it.
 */
@Serializable
data class Headset(
    val name: String,
    val pronouns: String,
)

object HeadsetAdapter : AbstractLegacyStringAdapter<List<Headset>>(ListSerializer(Headset.serializer())) {
    override fun fromPrimitive(primitive: JsonPrimitive): JsonElement? {
        if (primitive.content.isEmpty())
            return null
        return buildJsonObject {
            put("name", primitive.content)
            put("pronouns", "")
        }
    }
}