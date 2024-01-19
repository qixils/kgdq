package dev.qixils.gdq.v2.models

data class Page<M>(
    /**
     * Count of total available items.
     * May be greater than the size of [results].
     */
    val count: Int,
    /**
     * The URL for the next page of results.
     */
    val next: String,
    /**
     * The URL for the previous page of results.
     */
    val previous: String,
    /**
     * The entries on this page.
     */
    val results: List<M>,
) {

}
