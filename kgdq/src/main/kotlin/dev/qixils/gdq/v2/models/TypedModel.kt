package dev.qixils.gdq.v2.models

abstract class TypedModel : IdentifiedModel() {
    abstract val type: String
}