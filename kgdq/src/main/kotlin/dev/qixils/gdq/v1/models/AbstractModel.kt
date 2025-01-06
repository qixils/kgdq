package dev.qixils.gdq.v1.models

import dev.qixils.gdq.v1.GDQ
import kotlinx.serialization.Transient

@Deprecated(message = "Use v2 API")
abstract class AbstractModel : Model {
    @Transient
    private var _api: GDQ? = null
    private var _id: Int? = null
    override val api: GDQ get() = _api!!
    override val id: Int get() = _id!!
    override fun init(api: GDQ, id: Int) {
        this._api = api
        this._id = id
    }
}