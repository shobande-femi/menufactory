package gateway

data class Request(
    val phoneNumber: String,
    val sessionId: String,
    val serviceCode: String?,
    val message: String,
    val operator: String?
)