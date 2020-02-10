package gateway.wrappers

import exceptions.CannotParseRequest
import gateway.Gateway
import gateway.Request

object AfricasTalking: Gateway {

    override val name: String = "AFRICAS_TALKING"

    data class AfricasTalkingRequest(val map: Map<String, String>) {
        val phoneNumber by map
        val sessionId by map
        val serviceCode by map
        val text by map
        val networkCode by map
    }

    override fun transform(request: Any): Request {
        try {
            val africasTalkingRequest = AfricasTalkingRequest(request as Map<String, String>)
            return Request(
                africasTalkingRequest.phoneNumber,
                africasTalkingRequest.sessionId,
                africasTalkingRequest.serviceCode,
                if (africasTalkingRequest.text.isEmpty()) africasTalkingRequest.text else africasTalkingRequest.text.split("*").last(),
                africasTalkingRequest.networkCode
            )
        } catch (e: NoSuchElementException) {
            throw CannotParseRequest("Request $request doesn't match $name format because ${e.message}")
        }
    }

    override suspend fun con(message: String): String {
        return "CON $message"
    }

    override suspend fun end(message: String): String {
        return "END $message"
    }
}