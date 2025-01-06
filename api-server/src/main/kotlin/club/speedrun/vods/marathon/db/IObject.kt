package club.speedrun.vods.marathon.db

import club.speedrun.vods.db.Identified
import dev.qixils.gdq.serializers.InstantAsString

interface IObject : Identified {
    val cachedAt: InstantAsString
}