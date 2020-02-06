package state

import gateway.Gateway

@StateMarker
class StateRunner(val gateway: Gateway) {

    suspend fun con(message: String): Any {
        return gateway.con(message)
    }

    suspend fun end(message: String): Any {
        return gateway.end(message)
    }
}