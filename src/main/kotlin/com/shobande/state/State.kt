package com.shobande.state

/**
 * State object
 *
 * @param name state's name
 * @param handler the handler contains definitions for running the state, and it's possible transitions
 */
@StateMarker
class State(val name: String, val handler: StateHandler)