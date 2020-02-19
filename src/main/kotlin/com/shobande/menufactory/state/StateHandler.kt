package com.shobande.menufactory.state

import com.shobande.menufactory.exceptions.NoStateRunner
import com.shobande.menufactory.exceptions.NoTerminatorInStateRunner
import com.shobande.menufactory.gateway.Request
import com.shobande.menufactory.session.Session

/**
 * Houses commands for running a state and handling the state's transitions
 *
 * @param stateName state's name
 * @param stateRunner an instance of [StateRunner]. House commands for running a state
 * @param stateTransitions an instance of [StateTransitions]. Within this are state transition definitions
 *
 * @property runner the state's runner. The return value the runner is what is displayed to the user when this state is visited
 */
@StateMarker
class StateHandler(val stateName: String, private val stateRunner: StateRunner, val stateTransitions: StateTransitions) {
    lateinit var runner: suspend StateRunner.(request: Request) -> Any

    /**
     * Registers a state's runner.
     * You can think of running a state as determining what the state should display when a user visits the state.
     * The return value the runner is what is displayed to the user.
     *
     * @param runner lambda representing the runner
     */
    fun run(runner: suspend StateRunner.(request: Request) -> Any) {
        this.runner = runner
    }

    /**
     * Simply invokes the state's previously registered runner.
     * Redirects to another state if the runner returns a [State] object
     *
     * @param request [Request] object
     * @param session session handler for storing and retrieving information
     *
     * @return a wrapper around the actual response and a boolean signifying if the calling state is a final state or not.
     *
     * @throws
     * [NoTerminatorInStateRunner] when a state runner is defined but doesn't end with a call to any of:
     * `con`, `end`, `goTo` or `goToStart`
     * [NoStateRunner] when no runner has been registered
     */
    suspend fun handle(request: Request, session: Session): ResultWrapper {

        if (::runner.isInitialized) {

            return when(val result = stateRunner.runner(request)) {

                // if the return value is a Result wrapper, we know that con or end was called, hence return that result
                is ResultWrapper -> {
                    session.setPreviousStateName(request.sessionId, stateName)
                    result
                }

                // some sort of recursive call to handle state redirection
                // goTo returns a state object
                // hence if the return value of the state runner is of type state, we know we are redirecting to another state
                // call the target state's runner
                is State -> {
                    result.handler.handle(request, session)
                }

                else -> {
                    throw NoTerminatorInStateRunner("state runner end with a call to `con`, `end`, `goTo` or `goStart`")
                }
            }
        } else {
            throw NoStateRunner("No runner has been configured for $stateName state")
        }
    }

    /**
     * Registers a state's possible transitions
     *
     * @param mapping lambda containing transition mappings
     */
    fun transitions(mapping: StateTransitions.() -> Unit) {
        stateTransitions.mapping()
    }

    /**
     * Set the default next state
     *
     * @param name name of the state to default to
     */
    fun defaultNextState(name: String) {
        this.stateTransitions.defaultNextStateName = name
    }
}
