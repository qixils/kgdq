package dev.qixils.gdq.v2.models

abstract class IdentifiedModel : Model() {
    abstract val id: Int
}