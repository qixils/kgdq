package dev.qixils.horaro

class StatusCodeException(
    val statusCode: Int,
    message: String = "Invalid status code $statusCode"
) : RuntimeException(message)