package dev.qixils.gdq.v2.models

import dev.qixils.gdq.BidState
import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Bid(
    override val type: String,
    override val id: Int,
    val name: String,
    @SerialName("speedrun") val runId: Int?,
    val state: BidState,
    @SerialName("parent") val parentId: Int? = null,
    val description: String = "",
    @SerialName("shortdescription") val shortDescription: String = "",
    val goal: Double? = null,
    val total: Double = 0.0,
    val count: Int = 0,
    val repeat: Double? = null,
    val chain: Boolean = false,
    @SerialName("istarget") val isTarget: Boolean = false,
    val pinned: Boolean = false,
    @SerialName("allowuseroptions") val allowsUserOptions: Boolean = false,
    @SerialName("revealedtime") val revealedTime: InstantAsString? = null,
    val level: Int = 0,
    @SerialName("option_max_length") val optionMaxLength: Int? = null,
) : TypedModel() {

    suspend fun fetchRun(): Run? = runId?.let { api.getRun(it) }
    suspend fun fetchParent(): Bid? = parentId?.let { api.getBid(it) }
}