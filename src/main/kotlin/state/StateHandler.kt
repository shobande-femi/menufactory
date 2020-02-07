package state

import exceptions.NoStateRunner
import gateway.Request

@StateMarker
class StateHandler(val stateName: String, val stateRunner: StateRunner) {
    var defaultNextStateName = stateName
    val transitions = mutableMapOf<String, String>()
    lateinit var runner: suspend StateRunner.(request: Request) -> Any

    fun run(runner: suspend StateRunner.(request: Request) -> Any) {
        this.runner = runner
    }

    suspend fun run(request: Request): Any {
        if (::runner.isInitialized) {
            return runner(stateRunner, request)
        } else {
            throw NoStateRunner("No runner has been configured for $stateName state")
        }
    }

    fun transitions(mapping: () -> Unit) {
        mapping()
    }

    fun defaultNextState(name: String) {
        defaultNextStateName = name
    }

    fun nextState(input: String): String {
        return transitions[input]
            ?: transitions.getOrDefault(transitions.keys.firstOrNull { input.matches(it.toRegex()) }, defaultNextStateName)
    }

    infix fun String.to(stateName: String) {
        transitions[this] = stateName
    }

    infix fun String.to_(stateEnum: Enum<*>) {
        transitions[this] = stateEnum.name
    }

    infix fun String.to(stateResolver: () -> String) {
        transitions[this] = stateResolver()
    }

    /**
     * Appending an underscore to this method is a hack. Here's the reasoning.
     *
     * Kotlin's compiler doesn't recognize the receiver's return type as part of this method's signature.
     * The following 2 methods result in the exact same signature, leading to a compile time error.
     *
     * infix fun String.to(stateResolver: () -> String) {
     *     transitions[this] = stateResolver()
     * }
     * AND
     * infix fun String.to(stateResolver: () -> Enum<*>) {
     *     transitions[this] = stateResolver().name
     * }
     *
     * This should be solved from Kotlin 1.4
     *
     * For uniformity, every transition mapping method that accepts enumerations have an appended underscore
     */
    infix fun String.to_(stateResolver: () -> Enum<*>) {
        transitions[this] = stateResolver().name
    }
}
