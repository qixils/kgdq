package dev.qixils.gdq

import dev.qixils.gdq.models.Model
import dev.qixils.gdq.models.Wrapper

interface Hook<M : Model> {
    fun handle(item: Wrapper<M>)
}