package state

import exceptions.CannotMapTransitions
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

    infix fun String.to(stateEnum: Enum<*>) {
        transitions[this] = stateEnum.name
    }

    /**
     * Kotlin's compile doesn't recognize the receiver's return type as part of this method's signature
     *
     * Hence, having following 2 methods would throw an error
     *
     * infix fun String.to(stateResolver: () -> String) {
     *     transitions[this] = stateResolver()
     * }
     * AND
     * infix fun String.to(stateResolver: () -> Enum<*>) {
     *     transitions[this] = stateResolver().name
     * }
     */
    inline infix fun <reified T> String.to(stateResolver: () -> T) {
        when (T::class) {
            String::class -> {
                transitions[this] = stateResolver() as String
            }
            Enum::class -> {
                transitions[this] = (stateResolver() as Enum<*>).name
            }
            else -> {
                throw CannotMapTransitions("Cannot map transitions. Please ensure that destination is of type $String or $Enum")
            }
        }
    }
}
