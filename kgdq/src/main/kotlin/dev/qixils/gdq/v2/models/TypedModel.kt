package dev.qixils.gdq.v2.models

abstract class TypedModel: Model() {
    abstract val type: String
    abstract val id: Int
}