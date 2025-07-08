package club.speedrun.vods.utils

val skipCache = mutableSetOf(
    Regex("^$"),
    Regex("the checkpoint", setOf(RegexOption.IGNORE_CASE)),
    Regex("(?:\\s|^)recap(?:\\s|$)", setOf(RegexOption.IGNORE_CASE)),
    Regex("(?:\\s|^)finale$", setOf(RegexOption.IGNORE_CASE)),
    Regex("unknown game", setOf(RegexOption.IGNORE_CASE)),
    Regex("no runs known for this event", setOf(RegexOption.IGNORE_CASE)),
    Regex("bonus game", setOf(RegexOption.IGNORE_CASE)),
    Regex("tasbot", setOf(RegexOption.IGNORE_CASE)),
    Regex("the checkpoint", setOf(RegexOption.IGNORE_CASE)),
)

fun isSkipGame(game: String) = skipCache.any { it.containsMatchIn(game) }