package com.shobande.gateway.wrappers

import com.shobande.exceptions.CannotParseRequest
import com.shobande.gateway.Gateway
import com.shobande.gateway.Request

/**
 * Implementation of Nsano's Gateway
 */
object Nsano : Gateway {

    override val name: String = "NSANO"

    /**
     * This mirrors requests received from Nsano
     *
     * @param map simple map representing the JSON object received. The class properties are populated from this map
     *
     * @property msisdn phone number of the user interacting with the ussd app
     * @property msg most recent user input. When the user first dials your USSD code, [msg] is your USSD code
     * @property network mobile operator of the phoneNumber interacting with your ussd app
     */
    data class NsanoRequest(val map: Map<String, String>) {
        val msisdn by map
        val msg by map
        val network by map
    }

    /**
     * Represents the response object the Nsano Gateway expects
     *
     * @param action Possible values are:
     * input: responds without ending the USSD session
     * prompt: responds while ending the USSD session
     * @param menus: text to display to the user
     */
    data class NsanoResponse(
        val action: String,
        val menus: String
    )

    /**
     * Transforms request formats from Nsano specific request into a standard [Request] object.
     * In this case, the request is cast to a map, then an instance of [NsanoRequest] is generated from that map.
     *
     * Note:
     * Nsano doesn't provide a sessionId, hence we use the msisdn as the sessionId
     * Nsano doesn't provide the ussdCode, hence we set this as null
     *
     * @param request the gateway request. An instance of [NsanoRequest] would be generated from this
     * @return [Request] standard request object
     *
     * @throws [CannotParseRequest] Thrown when an instance of [NsanoRequest] cannot be generated from [request]
     *
     * @return a standard [Request]
     */
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

    /**
     * For Nsano, to send a response without ending the USSD session, set [NsanoResponse.action] to "input"
     *
     * @param message text to display to the user
     */
    override suspend fun con(message: String): NsanoResponse {
        return NsanoResponse("input", message)
    }

    /**
     * For Nsano, to send a response while ending the USSD session, set [NsanoResponse.action] to "prompt"
     *
     * @param message text to display to the user
     */
    override suspend fun end(message: String): NsanoResponse {
        return NsanoResponse("prompt", message)
    }
}