package club.speedrun.vods.marathon.db

import dev.qixils.gdq.serializers.InstantAsString

interface TimedObject : IObject {
    val startsAt: InstantAsString
}