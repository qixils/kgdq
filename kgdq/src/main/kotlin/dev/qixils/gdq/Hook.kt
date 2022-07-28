package dev.qixils.gdq

import dev.qixils.gdq.models.Model
import dev.qixils.gdq.models.Wrapper

interface Hook<M : Model> {
    suspend fun handle(item: Wrapper<M>)
}