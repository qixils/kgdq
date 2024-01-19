package dev.qixils.gdq.v1.models

import dev.qixils.gdq.v1.GDQ
import kotlinx.serialization.Transient

interface Model {

    @Transient var api: GDQ?
    var id: Int?

    fun isValid(): Boolean {
        return true
    }
}