package dev.qixils.gdq

import kotlinx.serialization.Serializable

@Serializable
enum class BidState {
    CLOSED,
    OPENED
}
