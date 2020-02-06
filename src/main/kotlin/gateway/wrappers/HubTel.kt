package gateway.wrappers

import exceptions.CannotParseRequest
import gateway.Gateway
import gateway.Request

object HubTel : Gateway {

    override val name: String = "HUBTEL"

    data class HubTelRequest(val map: Map<String, String>) {
        val Mobile by map
        val SessionId by map
        val ServiceCode by map
        val Type by map
        val Message by map
        val Operator by map
        val Sequence by map
        val ClientState by map
    }

    data class HubTelResponse(
        val Type: String,
        val Message: String
    )

    override fun transform(request: Any): Request {
        try {
            val hubTelRequest = HubTelRequest(request as Map<String, String>)
            return Request(
                hubTelRequest.Mobile,
                hubTelRequest.SessionId,
                hubTelRequest.ServiceCode,
                hubTelRequest.Message,
                hubTelRequest.Operator
            )
        } catch (e: NoSuchElementException) {
            throw CannotParseRequest("Request $request doesn't match ${AfricasTalking.name} format because ${e.message}")
        }
    }

    override suspend fun con(message: String): HubTelResponse {
        return HubTelResponse("Response", message)
    }

    override suspend fun end(message: String): HubTelResponse {
        return HubTelResponse("Release", message)
    }
}