package club.speedrun.vods.db

import java.util.*

fun Random.nextBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    nextBytes(bytes)
    return bytes
}