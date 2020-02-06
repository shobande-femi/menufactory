package gateway.wrappers

import exceptions.CannotParseRequest
import gateway.Gateway
import gateway.Request

object Nsano : Gateway {

    override val name: String = "NSANO"

    data class NsanoRequest(val map: Map<String, String>) {
        val msisdn by map
        val msg by map
        val network by map
    }

    data class NsanoResponse(
        val action: String,
        val menus: String
    )

    override fun transform(request: Any): Request {
        try {
            val nsanoRequest = NsanoRequest(request as Map<String, String>)
            return Request(
                nsanoRequest.msisdn,
                nsanoRequest.msisdn,
                null,
                nsanoRequest.msg,
                nsanoRequest.network
            )
        } catch (e: NoSuchElementException) {
            throw CannotParseRequest("Request $request doesn't match ${AfricasTalking.name} format because ${e.message}")
        }
    }

    override suspend fun con(message: String): NsanoResponse {
        return NsanoResponse("input", message)
    }

    override suspend fun end(message: String): NsanoResponse {
        return NsanoResponse("prompt", message)
    }
}