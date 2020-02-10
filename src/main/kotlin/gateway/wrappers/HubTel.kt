package gateway.wrappers

import exceptions.CannotParseRequest
import gateway.Gateway
import gateway.Request

/**
 * Implementation of Hubtel's Gateway
 */
object HubTel : Gateway {

    override val name: String = "HUBTEL"

    /**
     * This mirrors requests received from Hubtel
     *
     * @param map simple map representing the JSON object received. The class properties are populated from this map
     *
     * @property Mobile phone number of the user interacting with the ussd app
     * @property SessionId UUID representing the current user session. This stays the same through until the session ends
     * @property ServiceCode your app's USSD code
     * @property Type **Not Used**
     * According to Hubtel's documentation:
     * Indicates the type of USSD Request. Possible values are: Initiation:: indicates the first message in a USSD Session.
     * Response:: indicates the subsequent response in an already existing USSD session.
     * Release:: indicates that the subscriber is ending the USSD session.
     * Timeout:: indicates that the USSD session has timed out.
     * @property Message most recent user input. When the user first dials your USSD code, [Message] is your USSD code
     * @property Operator mobile operator of the phoneNumber interacting with your ussd app
     * @property Sequence **Not Used**
     * According to Hubtel's documentation:
     * Indicates the position of the current message in the USSD session.
     * @property ClientState **Not Used**
     * According to Hubtel's documentation:
     * Represents data that API client asked API service to send from the previous USSD request.
     * This data is only sent in the current request and is then discarded.
     */
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

    /**
     * Represents the response object the Hubtel Gateway expects
     *
     * @param Type Possible values are:
     * Response: responds without ending the USSD session
     * Release: responds while ending the USSD session
     * @param Message: text to display to the user
     */
    data class HubTelResponse(
        val Type: String,
        val Message: String
    )

    /**
     * Transforms request formats from Hubtel specific request into a standard [Request] object.
     * In this case, the request is cast to a map, then an instance of [HubTelRequest] is generated from that map.
     *
     * @param request the gateway request. An instance of [HubTelRequest] would be generated from this
     * @return [Request] standard request object
     *
     * @throws [CannotParseRequest] Thrown when an instance of [HubTelRequest] cannot be generated from [request]
     *
     * @return a standard [Request]
     */
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
            throw CannotParseRequest("Request $request doesn't match $name format because ${e.message}")
        }
    }

    /**
     * For Hubtel, to send a response without ending the USSD session, set [HubTelResponse.Type] to "Response"
     *
     * @param message text to display to the user
     */
    override suspend fun con(message: String): HubTelResponse {
        return HubTelResponse("Response", message)
    }

    /**
     * For Hubtel, to send a response while ending the USSD session, set [HubTelResponse.Type] to "Release"
     *
     * @param message text to display to the user
     */
    override suspend fun end(message: String): HubTelResponse {
        return HubTelResponse("Release", message)
    }
}