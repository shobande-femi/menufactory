package state

import gateway.Gateway

/**
 * Houses commands for running a state.
 * You can think of running a state as determining what the state should display when a user visits the state.
 *
 * @param gateway Specifies the gateway implementation to be used for parsing and responding to requests.
 */
@StateMarker
class StateRunner(val gateway: Gateway) {

    /**
     * Sends a response without ending the current user session
     *
     * @param message text to display to the user
     */
    suspend fun con(message: String): Any {
        return gateway.con(message)
    }

    /**
     * Sends a response while ending the current user session
     *
     * @param message text to display to the user
     */
    suspend fun end(message: String): Any {
        return gateway.end(message)
    }
}