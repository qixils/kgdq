package club.speedrun.vods

import java.util.*

fun <T> Appendable.appendElement(element: T, transform: ((T) -> CharSequence)?) {
    when {
        transform != null -> append(transform(element))
        element is CharSequence? -> append(element)
        element is Char -> append(element)
        else -> append(element.toString())
    }
}

fun <T, A : Appendable> naturalJoinTo(buffer: A, content: List<T>, transform: ((T) -> CharSequence)? = null): A {
    if (content.isEmpty()) return buffer

    buffer.appendElement(content.first(), transform)

    if (content.size == 1)
        return buffer

    for (i in 1 until content.size - 1)
        buffer.append(", ").appendElement(content[i], transform)

    if (content.size > 2)
        buffer.append(',')

    buffer.append(" and ").appendElement(content.last(), transform)

    return buffer
}

fun <T, A : Appendable> List<T>.naturalJoinTo(buffer: A, prefix: CharSequence = "", postfix: CharSequence = "", transform: ((T) -> CharSequence)? = null): A {
    buffer.append(prefix)
    naturalJoinTo(buffer, this, transform)
    buffer.append(postfix)
    return buffer
}

fun <T> List<T>.naturalJoinToString(prefix: CharSequence = "", postfix: CharSequence = "", transform: ((T) -> CharSequence)? = null): String {
    val buffer = StringBuilder()
    naturalJoinTo(buffer, prefix, postfix, transform)
    return buffer.toString()
}

fun Random.nextBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    nextBytes(bytes)
    return bytes
}


