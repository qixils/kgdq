package dev.qixils.gdq.v2.models

import dev.qixils.gdq.v2.DonationTracker
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Transient
import org.jetbrains.annotations.ApiStatus.Internal

abstract class Model {

    @Transient private var _api: DonationTracker? = null
    protected var serializer: KSerializer<out Model>? = null
    protected val api: DonationTracker get() = _api!!

    @Internal
    open fun init(api: DonationTracker, serializer: KSerializer<out Model>?) {
        this._api = api
        this.serializer = serializer
    }
}