@file:Suppress("UNCHECKED_CAST")

package dev.qixils.gdq.v2.models

import dev.qixils.gdq.v2.DonationTracker
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
data class Page<M : Model>(
    /**
     * Count of total available items.
     * May be greater than the size of [results].
     */
    val count: Int,
    /**
     * The URL for the next page of results.
     */
    val next: String?,
    /**
     * The URL for the previous page of results.
     */
    val previous: String?,
    /**
     * The entries on this page.
     */
    val results: List<M>,
) : Model() {

    constructor() : this(0, null, null, emptyList())

    override fun init(api: DonationTracker, serializer: KSerializer<out Model>?) {
        super.init(api, serializer)
        results.forEach { it.init(api, null) }
    }

    private suspend fun fetch(url: String?): Page<M>? {
        val httpUrl = url?.toHttpUrlOrNull() ?: return null
        return serializer?.let { api.getPage(httpUrl, it) as Page<M> }
    }

    /**
     * Fetches the next page of results.
     */
    suspend fun fetchNext() = fetch(next)

    /**
     * Fetches the previous page of results.
     */
    suspend fun fetchPrevious() = fetch(previous)

    /**
     * Fetches all available results.
     */
    suspend fun fetchAll(): List<M> {
        val results = this.results.toMutableList()
        var current: Page<M>? = this
        while (current?.next != null) {
            current = fetchNext()
            current?.let { results.addAll(current.results) }
        }
        return results
    }
}
