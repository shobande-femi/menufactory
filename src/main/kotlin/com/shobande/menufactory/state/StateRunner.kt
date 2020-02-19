package com.shobande.menufactory.state

import com.shobande.menufactory.gateway.Gateway

/**
 * Houses commands for running a state.
 * You can think of running a state as determining what the state should display when a user visits the state.
 *
 * @param gateway Specifies the gateway implementation to be used for parsing and responding to requests.
 */
@StateMarker
class StateRunner(private val gateway: Gateway) {

    /**
     * Sends a response without ending the current user session
     *
     * @param message text to display to the user
     *
     * @return @return a [ResultWrapper]. [ResultWrapper.finalState] is set to false to specify that whatever state that calls
     * this method is not a final state. Hence, session info would be retained.
     * [ResultWrapper.result] is the actual response.
     */
    suspend fun con(message: String): ResultWrapper {
        return ResultWrapper(false, gateway.con(message))
    }

    /**
     * Sends a response while ending the current user session
     *
     * @param message text to display to the user
     *
     * @return a [ResultWrapper]. [ResultWrapper.finalState] is set to true to specify that whatever state that calls
     * this method is final state. The session info for the current sessionId would be cleared
     * [ResultWrapper.result] is the actual response.
     */
    suspend fun end(message: String): ResultWrapper {
        return ResultWrapper(true, gateway.end(message))
    }
}