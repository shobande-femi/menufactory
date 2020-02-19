package com.shobande.menufactory.session.wrappers

import com.shobande.menufactory.exceptions.ReservedStateName
import com.shobande.menufactory.session.Session

/**
 * Handler for in-memory session.
 *
 * @property startStateName name of the start state.
 * This name will be a reserved for the start state only. Any other state definition that tries to use this name
 * will throw a [ReservedStateName] exception.
 */
class InMemorySession(override val startStateName: String = "__START__") : Session {

    private val session = mutableMapOf<String, MutableMap<String, Any>>()

    /**
     * Initializes a new session.
     * Invoked internally before any state is run.
     * If there is an active session for the specified sessionId, initialization is skipped
     *
     * @param sessionId user's session Id
     */
    override fun start(sessionId: String) {
        if (!session.containsKey(sessionId)) session[sessionId] = mutableMapOf()
    }

    /**
     * Stores a key-value pair in the current user session
     *
     * @param sessionId user's session Id
     * @param key
     * @param value
     */
    override fun set(sessionId: String, key: String, value: Any) {
        session[sessionId]?.set(key, value)
    }

    /**
     * Retrieves the value for a given key from the current user session
     *
     * @param sessionId user's session Id
     * @param key
     */
    override fun get(sessionId: String, key: String): Any? {
        return session[sessionId]?.get(key)
    }

    /**
     * Deletes a user session. Invoked internally by final states
     *
     * @param sessionId user's session Id
     */
    override fun end(sessionId: String) {
        session.remove(sessionId)
    }
}