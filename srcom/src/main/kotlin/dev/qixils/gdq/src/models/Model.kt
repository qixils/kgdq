package dev.qixils.gdq.src.models

import kotlinx.serialization.Serializable

sealed interface Model {
    val id: String
}

@Serializable
data class Link(
    val rel: String,
    val uri: String,
)
