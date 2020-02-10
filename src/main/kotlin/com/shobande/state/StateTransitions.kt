package com.shobande.state

import com.shobande.gateway.Request

/**
 *
 * @param defaultNextStateName the state to fallback to when transition cannot be determined for a given user input
 * @property transitions map that matches user inputs to next states.
 * Map keys could be exact matches to user input or regular expression matches to user inputs
 */
@StateMarker
class StateTransitions(var defaultNextStateName: String) {
    private val transitions = mutableMapOf<String, suspend (request: Request) -> String>()

    /**
     * Adds a transition definition.
     * The string upon which this method is called may be an exact match or a regular expression.
     *
     * @param stateName name of the state to transition to
     */
    infix fun String.to(stateName: String) {
        transitions[this] = { stateName }
    }

    /**
     * Adds a transition definition.
     * The string upon which this method is called may be an exact match or a regular expression.
     *
     * @param transitionFunction a lambda that returns the next state's name. In this lambda, there is access to the
     * [Request] object, hence fancy logic may be performed to determine the next state
     */
    infix fun String.to(transitionFunction: suspend (request: Request) -> String) {
        transitions[this] = transitionFunction
    }

    /**
     * Fetch the next state for a given user input.
     * The order of precedence is
     * 1. Exact Match
     * 2. Regular Expression
     * 3. Default Next State
     *
     * For any given user input, if there is an exact match in [transitions], then that is returned.
     * Else, it tries to match the input to regular expression defined in [transitions].
     * If it still cannot find a matching transition, it defaults to [defaultNextStateName]
     *
     * @param request request from gateway
     *
     * @return name of the next state
     */
    suspend fun nextState(request: Request): String {
        return transitions[request.message]?.invoke(request)
            ?: transitions[transitions.keys.firstOrNull { request.message.matches(it.toRegex()) }]?.invoke(request)
            ?: defaultNextStateName
    }
}