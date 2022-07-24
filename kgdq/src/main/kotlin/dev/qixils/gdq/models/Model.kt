package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ
import kotlinx.serialization.Transient

interface Model {
    suspend fun loadData(api: GDQ) {
    }
}