package menu

import exceptions.CannotResolveNextState
import exceptions.NoStartState
import exceptions.StateNotFound
import gateway.wrappers.AfricasTalking
import gateway.Gateway
import gateway.Request
import state.State
import state.StateHandler
import state.StateRunner

@MenuMarker
class Menu(val name: String, val gateway: Gateway = AfricasTalking) {
    val startStateName = "START"
    val states = mutableMapOf<String, State>()
    val session = mutableMapOf<String, String>()

    companion object {
        suspend fun menu(name: String, gateway: Gateway = AfricasTalking, init: suspend Menu.() -> Unit): Menu {
            val menu = Menu(name, gateway = gateway)
            menu.init()
            return menu
        }
    }

    private suspend fun initState(name: String, handler: suspend StateHandler.() -> Unit): State {
        val state = State(name, initStateHandler(name, handler))
        states[state.name] = state
        return state
    }

    private suspend fun initStateHandler(name:String, init: suspend StateHandler.() -> Unit): StateHandler {
        val stateHandler = StateHandler(name, StateRunner(gateway))
        stateHandler.init()
        return stateHandler
    }

    suspend fun state(name: String, handler: suspend StateHandler.() -> Unit) = initState(name, handler)

    suspend fun <T : Enum<T>> state(name: Enum<T>, handler: suspend StateHandler.() -> Unit) = initState(name.name, handler)

    suspend fun startState(handler: suspend StateHandler.() -> Unit) = initState(startStateName, handler)

    private fun previousState(request: Request): State {
        return if (session.containsKey(request.sessionId)) {
            states[session[request.sessionId]] ?: throw StateNotFound("Cannot find state with name: ${session[request.sessionId]}")
        } else {
            session[request.sessionId] = startStateName
            states[startStateName] ?: throw NoStartState("No start state has been configure for $name menu")
        }
    }

    suspend fun run(request: Any, responder: suspend (result: Any) -> Any) {
        val transformedRequest = gateway.transform(request)

        val previousState = previousState(transformedRequest)

        val currentState = states[previousState.handler.nextState(transformedRequest.message)]
            ?: throw CannotResolveNextState("${previousState.name} state cannot resolve next state for input ${transformedRequest.message}")

        session[transformedRequest.sessionId] = currentState.name

        val result = currentState.handler.run(transformedRequest)
        responder(result)
    }
}