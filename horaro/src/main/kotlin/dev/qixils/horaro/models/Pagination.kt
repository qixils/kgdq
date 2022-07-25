package dev.qixils.horaro.models

import dev.qixils.horaro.Horaro
import kotlinx.serialization.KSerializer

/**
 * Data about the pagination of a [ListResponse].
 */
sealed interface Pagination<R : ListResponse<*, R>> : Linkable {

    /**
     * The current offset of the pagination.
     */
    val offset: Int

    /**
     * The maximum number of items per page.
     */
    val max: Int

    /**
     * The number of items in the current page.
     */
    val size: Int

    /**
     * The serializer used to fetch the next page.
     */
    val serializer: KSerializer<R>

    /**
     * Calculates the next page's offset.
     *
     * @return the next page's offset
     */
    fun nextOffset(): Int {
        return offset + max
    }

    /**
     * Fetches the URL of the next page.
     *
     * @return the URL of the next page
     */
    fun nextUrl(): String? {
        return links.firstOrNull { it.type == LinkType.NEXT }?.uri
    }

    /**
     * Returns the next page of items or null if this is the last page.
     *
     * @return the next page of items or null if this is the last page
     */
    suspend fun next(): R? {
        if (size < max) return null
        val url = nextUrl() ?: return null
        val next = Horaro.get(url, serializer)
        if ((next.pagination?.size ?: 0) > 0) return next
        return null
    }
}
