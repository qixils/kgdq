package dev.qixils.gdq.src.models

import kotlinx.serialization.Serializable

@Serializable
data class Response<T : Model>(
    val data: List<T>,
    val pagination: Pagination
)

@Serializable
data class Pagination(
    val offset: Int,
    val max: Int,
    val size: Int,
    val links: List<Link>
)
