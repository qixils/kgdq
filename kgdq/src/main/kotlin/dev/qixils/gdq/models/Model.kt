package dev.qixils.gdq.models

import dev.qixils.gdq.GDQ

interface Model {
    suspend fun loadData(api: GDQ, id: Int) {
    }

    fun isValid(): Boolean {
        return true
    }

    /**
     * Instructs the model to skip performing web requests to load its data.
     * This may result in some values unexpectedly being null.
     */
    fun skipLoad() {
    }
}