package com.shobande.menufactory.menu

import com.shobande.menufactory.exceptions.CannotResolveNextState
import com.shobande.menufactory.exceptions.ReservedStateName
import com.shobande.menufactory.exceptions.StateNotFound
import com.shobande.menufactory.gateway.Gateway
import com.shobande.menufactory.gateway.wrappers.AfricasTalking
import com.shobande.menufactory.session.Session
import com.shobande.menufactory.session.wrappers.InMemorySession
import com.shobande.menufactory.state.State
import com.shobande.menufactory.state.StateHandler
import com.shobande.menufactory.state.StateRunner
import com.shobande.menufactory.state.StateTransitions

/**
 * The USSD Menu
 *
 * @param name name our the USSD application
 * @param gateway specifies the gateway to be used. [AfricasTalking] by default
 * @param session implementation of [Session] for storing and retrieving information for the current session
 *
 * @property states map of state names to state objects
 */
@MenuMarker
class Menu(val name: String, val gateway: Gateway = AfricasTalking, val session: Session = InMemorySession()) {
    private val states = mutableMapOf<String, State>()

    companion object {
        /**
         * Menu Factory
         */
        suspend fun menu(name: String, gateway: Gateway = AfricasTalking, init: suspend Menu.() -> Unit): Menu {
            val menu = Menu(name, gateway = gateway)
            menu.init()
            return menu
        }
    }

    /**
     * Registers a state on the USSD menu
     *
     * @param name state's name
     * @param handler state's handler. The handler houses logic for running a state and handling the state's transitions
     *
     * @return the registered state
     */
    private suspend fun initState(name: String, handler: suspend StateHandler.() -> Unit): State {
        val state = State(name, initStateHandler(name, handler))
        states[state.name] = state
        return state
    }

    /**
     * Initialize a state's handler.
     * The handler houses logic for running a state and handling the state's transitions
     *
     * @param name name of the state owning the handler
     * @param init state's handler lambda
     */
    private suspend fun initStateHandler(name:String, init: suspend StateHandler.() -> Unit): StateHandler {
        val stateHandler = StateHandler(name, StateRunner(gateway), StateTransitions(name))
        stateHandler.init()
        return stateHandler
    }

    /**
     * Registers a state on the USSD menu
     *
     * @param name state's name
     * @param handler state's handler. The handler houses logic for running a state and handling the state's transitions
     *
     * @return the registered state
     */
    suspend fun state(name: String, handler: suspend StateHandler.() -> Unit) {
        if (name == session.startStateName) throw ReservedStateName("state name $name is reserved for internal use only")
        initState(name, handler)
    }

    /**
     * Registers the start state on the USSD menu
     *
     * @param handler state's handler. The handler houses logic for running a state and handling the state's transitions
     *
     * @return the registered state
     */
    suspend fun startState(handler: suspend StateHandler.() -> Unit) = initState(session.startStateName, handler)

    /**
     * Fetches the user's last visited state from the session.
     * If no previous state is found for the [sessionId], the start state is returned
     *
     * @param sessionId UUID representing the user's current session
     *
     * @return the last visited State or start state
     */
    private fun previousState(sessionId: String): State {
        val previousStateName = session.getPreviousStateName(sessionId)
        return states[previousStateName] ?: throw StateNotFound("Cannot find state with name: $previousStateName")
    }

    /**
     * This is the entry point for the USSD menu. Requests from gateways are processed here.
     * Firstly, the request is marshalled to the format specified by the chosen [gateway]
     * Then the session ID is used to fetch the user's last visited state.
     * The transitions defined in the handler of the last visited state is used to determine the next state.
     *
     * At the start of a user session, there is not previous state, hence the start state is returned as the previous state.
     * The start state then tries to handle the transition, but since the user input would be an empty string
     * or the ussd code (depending on the gateway), no matching transition would be found (as long as you have not
     * defined a transition on the start state that matches the user input provided by the gateway).
     * Hence, it will transition to the default next state. If the default next state has not been overridden,
     * the state will default to itself.
     * This may seem like an overkill, and it may be worth considering some circuit breaker pattern where on start of
     * ussd session, if the user input is an empty string or the ussd code, then return the start state.
     * At the moment, I have chosen not to do this because different gateways may have different values set as the input
     * on start of the ussd session. Some gateways use an empty string, some use the ussd code, others even use the ussd
     * code without the leading asterisk (*). The options are endless.
     *
     * @param request request sent to the USSD app
     * @param responder lambda used to send a response to the gateway
     */
    suspend fun handle(request: Any, responder: suspend (result: Any) -> Any) {
        val transformedRequest = gateway.transform(request)

        val previousState = previousState(transformedRequest.sessionId)

        val currentState = states[previousState.handler.stateTransitions.nextState(transformedRequest)]
            ?: throw CannotResolveNextState("${previousState.name} state cannot resolve next state for input ${transformedRequest.message}")

        val result = currentState.handler.handle(transformedRequest, session)
        responder(result)
    }

    /**
     * Redirect to another state
     *
     * @param stateName target state's name
     *
     * @return target [State] object
     */
    fun goTo(stateName: String): State {
        return states[stateName] ?: throw StateNotFound("Cannot find state with name: $stateName")
    }

    /**
     * Redirect to start state
     *
     * @return start [State] object
     */
    fun goToStart() = goTo(session.startStateName)
}