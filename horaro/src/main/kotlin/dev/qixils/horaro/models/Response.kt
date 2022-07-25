package dev.qixils.horaro.models

/**
 * A response that was wrapped by the Horaro API.
 */
sealed interface Response<M> {

    /**
     * The data that was returned by the API.
     */
    val data: M
}

/**
 * A response containing a collection of objects of type [M].
 */
sealed interface ListResponse<M, R : ListResponse<M, R>> : Response<List<M>> {

    /**
     * The pagination object which allows traversal through the pages of objects.
     * May be null if this is the only page.
     */
    val pagination: Pagination<R>?
}
