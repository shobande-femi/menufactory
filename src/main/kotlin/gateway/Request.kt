package gateway

data class Request(
    val phoneNumber: String,
    val sessionId: String,
    val ussdCode: String?,
    val message: String,
    val operator: String?
)