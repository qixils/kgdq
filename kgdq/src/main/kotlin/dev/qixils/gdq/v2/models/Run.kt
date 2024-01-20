package dev.qixils.gdq.v2.models

import dev.qixils.gdq.serializers.DurationAsString
import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Run(
    override val type: String,
    override val id: Int,
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
    val description: String = "",
    val category: String = "",
    val console: String = "",
    val runners: List<Runner> = listOf(),
    val hosts: List<Headset> = listOf(),
    val commentators: List<Headset> = listOf(),
    @SerialName("starttime") val startTime: InstantAsString? = null,
    @SerialName("endtime") val endTime: InstantAsString? = null,
    val order: Int,
    @SerialName("run_time") val runTime: DurationAsString,
    @SerialName("setup_time") val setupTime: DurationAsString,
    @SerialName("anchor_time") val anchorTime: InstantAsString? = null,
) : TypedModel()