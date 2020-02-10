package gateway

/**
 * A standardized request object.
 * All request from supported gateways would be marshalled to this object.
 *
 * @property phoneNumber phone number of the user interacting with the ussd app
 * @property sessionId UUID representing the current user session. This stays the same through until the session ends
 * @property ussdCode your app's USSD code
 * @property message most recent user input
 * @property operator mobile operator of the phoneNumber interacting with your ussd app
 */
data class Request(
    val phoneNumber: String,
    val sessionId: String,
    val ussdCode: String?,
    val message: String,
    val operator: String?
)