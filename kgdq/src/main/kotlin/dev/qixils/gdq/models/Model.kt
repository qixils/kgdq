package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ

interface Model {
    suspend fun loadData(api: GDQ) {
    }
}