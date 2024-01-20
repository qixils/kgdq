import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("UtilKt")

suspend fun Response.readBodyString(): String? {
    val deferred = CompletableDeferred<String?>()
    try {
        withContext(Dispatchers.IO) {
            if (code == 200)
                deferred.complete(body.string())
            else {
                logger.warn("Encountered $code on ${request.url}")
                deferred.complete(null)
            }
        }
    } catch (e: Exception) {
        logger.warn("Failed to read body on ${request.url}", e)
    } finally {
        closeQuietly()
    }
    return deferred.await() // TODO: does this make sense?? given the withContext?? i Don't Know
}

fun Map<String, String?>?.toSearchParams(): String {
    if (isNullOrEmpty()) return ""
    return buildString {
        entries.forEachIndexed { index, entry ->
            append(if (index == 0) '?' else '&')
            append(entry.key)
            if (!entry.value.isNullOrBlank()) {
                append('=')
                append(entry.value)
            }
        }
    }
}

class SearchParamsBuilder {
    private var params: String = ""
    private var count: Int = 0

    fun add(key: String, value: String? = null): SearchParamsBuilder {
        params += buildString {
            append(if (count++ == 0) '?' else '&')
            append(key)
            if (!value.isNullOrBlank()) append("=$value")
        }
        return this
    }

    fun addIfNotNull(key: String, value: String? = null): SearchParamsBuilder {
        if (value.isNullOrBlank()) return this
        params += buildString {
            append(if (count++ == 0) '?' else '&')
            append("$key=$value")
        }
        return this
    }

    fun build(): String = params
}

fun buildSearchParams(transform: SearchParamsBuilder.() -> Unit): String {
    val builder = SearchParamsBuilder()
    transform(builder)
    return builder.build()
}