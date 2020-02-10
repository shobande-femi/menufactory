package com.shobande.menu

import com.shobande.exceptions.CannotResolveNextState
import com.shobande.exceptions.NoStartState
import com.shobande.exceptions.StateNotFound
import com.shobande.gateway.Gateway
import com.shobande.gateway.wrappers.AfricasTalking
import com.shobande.state.State
import com.shobande.state.StateHandler
import com.shobande.state.StateRunner
import com.shobande.state.StateTransitions

/**
 * The USSD Menu
 *
 * @param name name our the USSD application
 * @param gateway specifies the gateway to be used. [AfricasTalking] by default
 *
 * @property startStateName name of the start state
 * @property states map of state names to state objects
 * @property session map of sessions IDs to last visited state
 */
@MenuMarker
class Menu(private val name: String, val gateway: Gateway = AfricasTalking) {
    private val startStateName = "__START__"
    private val states = mutableMapOf<String, State>()
    private val session = mutableMapOf<String, String>()

    companion object {
        /**
         * todo
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
    suspend fun state(name: String, handler: suspend StateHandler.() -> Unit) = initState(name, handler)

    /**
     * Registers the start state on the USSD menu
     *
     * @param handler state's handler. The handler houses logic for running a state and handling the state's transitions
     *
     * @return the registered state
     */
    suspend fun startState(handler: suspend StateHandler.() -> Unit) = initState(startStateName, handler)

    /**
     * Fetches the user's last visited state from the session.
     * If no previous state is found for the [sessionId], the start state is returned
     *
     * @param sessionId UUID representing the user's current session
     *
     * @return the last visited State or start state
     */
    private fun previousState(sessionId: String): State {
        return if (session.containsKey(sessionId)) {
            states[session[sessionId]] ?: throw StateNotFound("Cannot find state with name: ${session[sessionId]}")
        } else {
            session[sessionId] = startStateName
            states[startStateName] ?: throw NoStartState("No start state has been configure for $name menu")
        }
    }

    /**
     * This is the entry point for the USSD menu. Requests from gateways are processed here.
     * Firstly, the request is marshalled to the format specified by the chosen [gateway]
     * Then the session ID is used to fetch the user's last visited state.
     * The transitions defined in the handler of the last visited state is used to determine the next state.
     *
     * @param request request sent to the USSD app
     * @param responder lambda used to send a response to the gateway
     */
    suspend fun handle(request: Any, responder: suspend (result: Any) -> Any) {
        val transformedRequest = gateway.transform(request)

        val previousState = previousState(transformedRequest.sessionId)

        val currentState = states[previousState.handler.stateTransitions.nextState(transformedRequest)]
            ?: throw CannotResolveNextState("${previousState.name} state cannot resolve next state for input ${transformedRequest.message}")

        session[transformedRequest.sessionId] = currentState.name

        val result = currentState.handler.handle(transformedRequest)
        responder(result)
    }
}