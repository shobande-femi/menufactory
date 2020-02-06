package state

@StateMarker
class State(val name: String, val handler: StateHandler)