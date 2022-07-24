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
    @SerialName("option_max_length") val optionMaxLength: Int? = null,
    @Serializable(with = InstantSerializer::class) @SerialName("revealedtime") val revealedAt: Instant?,
    // @SerialName("biddependency") val bidDependency: ???, (this field is unused; can't be bothered to find what its type is)
    val total: Float,
    val count: Int,
    val pinned: Boolean = false,
    @SerialName("canonical_url") private var _canonicalUrl: String? = null,
    val public: String,
) : Model {

    @Transient private var api: GDQ? = null
    @Transient private var _event: Wrapper<Event>? = null
    @Transient private var _run: Wrapper<Run>? = null
    @Transient private var _parent: Wrapper<Bid>? = null

    override suspend fun loadData(api: GDQ, id: Int) {
        this.api = api
        // canonical URL fallback
        if (_canonicalUrl == null)
            _canonicalUrl = api.apiPath.replaceFirst("/search/", "/bid/", false) + id
    }

    val canonicalUrl: String get() = _canonicalUrl!!

    suspend fun event(): Wrapper<Event> {
        if (_event == null)
            _event = api!!.query(type = ModelType.EVENT, id = eventId).first()
        return _event!!
    }
    suspend fun run(): Wrapper<Run>? {
        if (_run == null && runId != null)
            _run = api!!.query(type = ModelType.RUN, id = runId).firstOrNull()
        return _run
    }
    suspend fun parent(): Wrapper<Bid>? {
        if (_parent == null && parentId != null)
            _parent = api!!.query(type = ModelType.BID, id = parentId).firstOrNull()
        return _parent
    }
}

@Serializable
enum class BidState {
    CLOSED,
    OPENED
}
