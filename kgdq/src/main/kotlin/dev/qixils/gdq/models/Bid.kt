package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.serializers.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Bid(
    @SerialName("event") private val eventId: Int,
    @SerialName("speedrun") val run: Wrapper<Run>,
    @SerialName("parent") private val parentId: Int?,
    val name: String,
    val state: BidState,
    val description: String,
    @SerialName("shortdescription") val shortDescription: String,
    val goal: Int?,
    @SerialName("istarget") val isTarget: Boolean,
    @SerialName("allowuseroptions") val allowUserOptions: Boolean,
    @SerialName("option_max_length") val optionMaxLength: Int?,
    @Serializable(with = InstantSerializer::class) @SerialName("revealedtime") val revealedAt: Instant,
    // @SerialName("biddependency") val bidDependency: ???, (this field is unused; can't be bothered to find what its type is)
    val total: Float,
    val count: Int,
    val pinned: Boolean,
    @SerialName("canonical_url") val canonicalUrl: String,
    val public: String,
) : Model {

    private var _event: Wrapper<Event>? = null
    private var _parent: Wrapper<Bid>? = null

    override suspend fun loadData(api: GDQ) {
        _event = api.query(type=ModelType.EVENT, id=eventId).first()
        _parent = if (parentId != null) api.query(type=ModelType.BID, id=parentId).first() else null
    }

    val event: Wrapper<Event> get() = _event!!
    val parent: Wrapper<Bid>? get() = _parent
}

@Serializable
enum class BidState {
    CLOSED,
    OPENED
}
