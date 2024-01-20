package dev.qixils.gdq.v2.models

import dev.qixils.gdq.serializers.InstantAsStringSerializer
import dev.qixils.gdq.serializers.ZoneIdAsString
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
class Event(
    override val type: String,
    override val id: Int,
    val short: String,
    val name: String,
    val hashtag: String,
    @Serializable(with = InstantAsStringSerializer::class) val datetime: Instant,
    val timezone: ZoneIdAsString,
) : TypedModel()