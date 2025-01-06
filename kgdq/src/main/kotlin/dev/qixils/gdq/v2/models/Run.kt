package dev.qixils.gdq.v2.models

import dev.qixils.gdq.serializers.DurationAsString
import dev.qixils.gdq.serializers.InstantAsString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Run(
    override val type: String,
    override val id: Int,
    val event: Event? = null, // todo: probably subclass, this is only on the global /runs/ endpoint
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("twitch_name") val twitchName: String = "",
    val description: String = "",
    val category: String = "",
    val console: String = "",
    @SerialName("release_year") val releaseYear: Int = 1970,
    val runners: List<Talent> = listOf(),
    val hosts: List<Talent> = listOf(),
    val commentators: List<Talent> = listOf(),
    @SerialName("starttime") val startTime: InstantAsString? = null,
    @SerialName("endtime") val endTime: InstantAsString? = null,
    val order: Int,
    @SerialName("run_time") val runTime: DurationAsString,
    @SerialName("setup_time") val setupTime: DurationAsString,
    @SerialName("anchor_time") val anchorTime: InstantAsString? = null,
    val coop: Boolean = false,
    @SerialName("video_links") val videoLinks: List<VideoLink> = listOf(),
) : TypedModel()
