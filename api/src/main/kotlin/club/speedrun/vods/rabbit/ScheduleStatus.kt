package club.speedrun.vods.rabbit

import club.speedrun.vods.Identified
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleStatus(
    override val id: String,
    var currentRun: String? = null, // Horaro run ID
    var usingGameScene: Boolean? = null // whether a game scene is being used
) : Identified {
    companion object {
        const val COLLECTION_NAME = "ScheduleStatus"
    }
}
