package dev.qixils.gdq

import dev.qixils.gdq.models.Model

data class Query<M : Model> (
    val type: ModelType<M>,
    val id: Int? = null,
    val event: Int? = null,
    val runner: Int? = null,
    val run: Int? = null,
    val offset: Int? = null,
) {
    fun asQueryString(): String {
        val params = mutableListOf("type=${type.id}")
        if (id != null) params.add("id=${id}")
        if (event != null) params.add("event=${event}")
        if (runner != null) params.add("runner=${runner}")
        if (run != null) params.add("run=${run}")
        if (offset != null) params.add("offset=${offset}")
        return params.joinToString("&")
    }
}
