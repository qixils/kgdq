package dev.qixils.gdq.src.models

import dev.qixils.gdq.serializers.InstantAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

sealed interface BaseGame : Model {
    val names: Names
    val abbreviation: String
    val weblink: String
}

@Serializable
data class BulkGame(
    override val id: String,
    override val names: Names,
    override val abbreviation: String,
    override val weblink: String,
) : BaseGame

@Serializable
data class FullGame(
    override val id: String,
    override val names: Names,
    override val abbreviation: String,
    override val weblink: String,
    val released: Int,
    val releaseDate: String? = null,
    val ruleset: Ruleset,
    val romhack: Boolean,
    // TODO: handle embedding
    val gametypes: List<String>,
    val platforms: List<String>,
    val regions: List<String>,
    val genres: List<String>,
    val engines: List<String>,
    val developers: List<String>,
    val moderators: Map<String, String>, // TODO create enum for latter
    @Serializable(with = InstantAsStringSerializer::class) val created: Instant? = null,
    val assets: Map<String, Asset?>, // TODO create enum for former
    val links: List<Link>,
) : BaseGame

@Serializable
data class Names(
    val international: String,
    val japanese: String?,
    val twitch: String?,
)

@Serializable
data class Ruleset(
    @SerialName("show-milliseconds") val showMilliseconds: Boolean,
    @SerialName("require-verification") val requireVerification: Boolean,
    @SerialName("require-video") val requireVideo: Boolean,
    @SerialName("run-times") val runTimes: List<String>,
    @SerialName("default-time") val defaultTime: String,
    @SerialName("emulators-allowed") val emulatorsAllowed: Boolean,
)

@Serializable
data class Asset(
    val uri: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)
