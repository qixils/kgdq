package dev.qixils.gdq.v1.models

import dev.qixils.gdq.BidState
import dev.qixils.gdq.serializers.InstantAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Bid(
    @SerialName("event") val eventId: Int,
    @SerialName("speedrun") val runId: Int?,
    @SerialName("parent") val parentId: Int?,
    val name: String,
    val state: BidState? = null, // unfortunately nullable
    val description: String,
    @SerialName("shortdescription") val shortDescription: String,
    val goal: Float?,
    @SerialName("istarget") val isTarget: Boolean,
    @SerialName("allowuseroptions") val allowUserOptions: Boolean,
    @SerialName("option_max_length") val optionMaxLength: Int? = null,
    @Serializable(with = InstantAsStringSerializer::class) @SerialName("revealedtime") val revealedAt: Instant?,
    // @SerialName("biddependency") val bidDependency: ???, (this field is unused; can't be bothered to find what its type is)
    val total: Float,
    val count: Int,
    val pinned: Boolean = false,
    @SerialName("canonical_url") private var _canonicalUrl: String? = null,
    val public: String,
) : AbstractModel() {

    val canonicalUrl: String
        get() = _canonicalUrl
            ?: (api.apiPath.replaceFirst("/search/", "/bid/", false) + id)

    suspend fun fetchEvent(): Event {
        return api.getEvent(eventId)!!
    }

    suspend fun fetchRun(): Run? {
        return runId?.let { api.getRun(it) }
    }

    suspend fun fetchParent(): Bid? {
        return parentId?.let { api.getBidParent(it) }
    }
}
