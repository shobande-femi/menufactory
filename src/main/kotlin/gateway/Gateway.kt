package gateway

import exceptions.CannotParseRequest

interface Gateway {

    val name: String

    @Throws(CannotParseRequest::class)
    fun transform(request: Any) : Request

    suspend fun con(message: String): Any

    suspend fun end(message: String): Any
}