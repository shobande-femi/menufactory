package state

import exceptions.NoStateRunner
import gateway.Request

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
class StateHandler(private val stateName: String, private val stateRunner: StateRunner, val stateTransitions: StateTransitions) {
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
     * Simply invokes the state's previously registered runner
     *
     * @param request [Request] object
     *
     * @throws [NoStateRunner] when no runner has been registered
     *
     */
    suspend fun handle(request: Request): Any {
        if (::runner.isInitialized) {
            return stateRunner.runner(request)
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
