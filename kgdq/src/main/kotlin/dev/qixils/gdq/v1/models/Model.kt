package dev.qixils.gdq.v1.models

import dev.qixils.gdq.v1.GDQ
import kotlinx.serialization.Transient
import org.jetbrains.annotations.ApiStatus.Internal

@Deprecated(message = "Use v2 API")
interface Model {

    @Transient val api: GDQ
    val id: Int

    /**
     * Initializes some internal values.
     */
    @Internal
    fun init(api: GDQ, id: Int)

    /**
     * Checks if this object contains all critical data.
     * Will always return `true` when constructed from publicly-facing methods.
     */
    @Internal
    fun isValid(): Boolean {
        return true
    }
}