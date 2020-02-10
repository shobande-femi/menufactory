package com.shobande.gateway.wrappers

import com.shobande.exceptions.CannotParseRequest
import com.shobande.gateway.Gateway
import com.shobande.gateway.Request

/**
 * Implementation of Africa's Talking Gateway
 */
object AfricasTalking: Gateway {

    override val name: String = "AFRICAS_TALKING"

    /**
     * This mirrors requests received from Africa's Talking
     *
     * @param map simple map representing the JSON object received. The class properties are populated from this map
     *
     * @property phoneNumber phone number of the user interacting with the ussd app
     * @property sessionId UUID representing the current user session. This stays the same through until the session ends
     * @property serviceCode your app's USSD code
     * @property text user input. When the user first dials your USSD code, [text] is an empty string.
     * Subsequently, it concatenates all the user input within the session with a * until the session ends.
     * Example: "1*2" means the user's input into the previous com.shobande.menu was "1", while the input into the current com.shobande.menu is "2"
     * @property networkCode mobile operator of the phoneNumber interacting with your ussd app
     */
    data class AfricasTalkingRequest(val map: Map<String, String>) {
        val phoneNumber by map
        val sessionId by map
        val serviceCode by map
        val text by map
        val networkCode by map
    }

    /**
     * Transforms request formats from Africa's Talking specific request into a standard [Request] object.
     * In this case, the request is cast to a map, then an instance of [AfricasTalkingRequest] is generated from that map.
     *
     * Note: Africa's Talking sends an asterisk separated string that represent the user's input from the start of the session.
     * Since we are only concerned about the latest user input, we can discard previous inputs.
     *
     * @param request the gateway request. An instance of [AfricasTalkingRequest] would be generated from this
     * @return [Request] standard request object
     *
     * @throws [CannotParseRequest] Thrown when an instance of [AfricasTalkingRequest] cannot be generated from [request]
     *
     * @return a standard [Request]
     */
    override fun transform(request: Any): Request {
        try {
            val africasTalkingRequest =
                AfricasTalkingRequest(request as Map<String, String>)
            return Request(
                africasTalkingRequest.phoneNumber,
                africasTalkingRequest.sessionId,
                africasTalkingRequest.serviceCode,
                if (africasTalkingRequest.text.isEmpty()) africasTalkingRequest.text else africasTalkingRequest.text.split(
                    "*"
                ).last(),
                africasTalkingRequest.networkCode
            )
        } catch (e: NoSuchElementException) {
            throw CannotParseRequest("Request $request doesn't match $name format because ${e.message}")
        }
    }

    /**
     * For Africa's Talking, to send a response without ending the USSD session, prepend the message with 'CON'
     *
     * @param message text to display to the user
     */
    override suspend fun con(message: String): String {
        return "CON $message"
    }

    /**
     * For Africa's Talking, to send a response while ending the USSD session, prepend the message with 'END'
     *
     * @param message text to display to the user
     */
    override suspend fun end(message: String): String {
        return "END $message"
    }
}