package dev.qixils.gdq.models

import dev.qixils.gdq.serializers.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Run(
    val event: Wrapper<Model>,
    val name: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("twitch_name") val twitchName: String,
    @SerialName("deprecated_runners") val deprecatedRunners: String,
    val console: String,
    val commentators: String,
    val description: String,
    @Serializable(with = InstantSerializer::class) @SerialName("starttime") val startTime: Instant,
    @Serializable(with = InstantSerializer::class) @SerialName("endtime") val endTime: Instant,
    val order: Int,
    @SerialName("run_time") val runTime: String,
    @SerialName("setup_time") val setupTime: String,
    val coop: Boolean,
    val category: String,
    @SerialName("release_year") val releaseYear: Int?,
    val runners: List<Int>, // TODO: create a utility method which returns a list of Wrapper<Runner>
    @SerialName("canonical_url") val canonicalUrl: String,
    val public: String
) : Model
