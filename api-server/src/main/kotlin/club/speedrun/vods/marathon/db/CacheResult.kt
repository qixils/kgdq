package club.speedrun.vods.marathon.db

data class CacheResult<O>(
    val obj: O,
    val isValid: Boolean,
)