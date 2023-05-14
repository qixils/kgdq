package dev.qixils.gdq

import dev.qixils.gdq.models.Model

data class Query<M : Model> (
    val type: ModelType<M>,
    val id: Int? = null,
    val event: Int? = null,
    val runner: Int? = null,
    val run: Int? = null,
    val name: String? = null,
    val offset: Int? = null,

    // TODO: https://github.com/GamesDoneQuick/donation-tracker/blob/master/tracker/search_filters.py#L108 _SpecificFields
) {
    fun asQueryString(): String {
        val params = mutableListOf("type=${type.id}")
        if (id != null) params.add("id=${id}")
        if (event != null) params.add("event=${event}")
        if (runner != null) params.add("runner=${runner}")
        if (run != null) params.add("run=${run}")
        if (name != null) params.add("name=${name}")
        if (offset != null) params.add("offset=${offset}")
        return params.joinToString("&")
    }
}
