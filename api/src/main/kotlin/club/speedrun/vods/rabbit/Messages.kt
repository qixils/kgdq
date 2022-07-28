package club.speedrun.vods.rabbit

import kotlinx.serialization.Serializable

sealed interface Message {
    val event: String
    val time: Time
}

@Serializable
data class SCActiveRunChanged(
    override val event: String,
    override val time: Time,
    val run: ActiveRun? = null,
) : Message

@Serializable
data class OBSSceneChanged(
    override val event: String,
    override val time: Time,
    val scene: String,
//    val action: Action? = null,
    val gameScene: Boolean
) : Message

//enum class Action {
//    START,
//    END
//}