package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import dev.qixils.gdq.ModelType
import dev.qixils.gdq.serializers.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

@Serializable
data class Bid(
    @SerialName("event") private val eventId: Int,
    @SerialName("speedrun") private val runId: Int?,
    @SerialName("parent") private val parentId: Int?,
    val name: String,
    val state: BidState,
    val description: String,
    @SerialName("shortdescription") val shortDescription: String,
    val goal: Float?,
    @SerialName("istarget") val isTarget: Boolean,
    @SerialName("allowuseroptions") val allowUserOptions: Boolean,
    @SerialName("option_max_length") val optionMaxLength: Int?,
    @Serializable(with = InstantSerializer::class) @SerialName("revealedtime") val revealedAt: Instant?,
    // @SerialName("biddependency") val bidDependency: ???, (this field is unused; can't be bothered to find what its type is)
    val total: Float,
    val count: Int,
    val pinned: Boolean,
    @SerialName("canonical_url") val canonicalUrl: String,
    val public: String,
) : Model {

    @Transient private var _event: Wrapper<Event>? = null
    @Transient private var _run: Wrapper<Run>? = null
    @Transient private var _parent: Wrapper<Bid>? = null

    override suspend fun loadData(api: GDQ) {
        _event = api.query(type=ModelType.EVENT, id=eventId).first()
        _run = if (runId != null) api.query(type=ModelType.RUN, id=runId).firstOrNull() else null
        _parent = if (parentId != null) api.query(type=ModelType.BID, id=parentId).firstOrNull() else null
    }

    val event: Wrapper<Event> get() = _event!!
    val run: Wrapper<Run>? get() = _run
    val parent: Wrapper<Bid>? get() = _parent
}

@Serializable
enum class BidState {
    CLOSED,
    OPENED
}
