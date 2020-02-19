package com.shobande.menufactory.state

/**
 * A wrapper around the result returned by a state runner
 *
 * @param finalState a boolean that states if the calling state is a final state or note
 * @param result the actual response to be returned
 */
data class ResultWrapper(
    val finalState: Boolean,
    val result: Any
)