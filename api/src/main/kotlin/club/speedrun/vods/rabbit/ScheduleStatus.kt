package club.speedrun.vods.rabbit

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleStatus(
    val _id: String,
    var currentRun: String? = null, // Horaro run ID
    var usingGameScene: Boolean? = null // whether a game scene is being used
) {
    companion object {
        const val COLLECTION_NAME = "ScheduleStatus"
    }
}
