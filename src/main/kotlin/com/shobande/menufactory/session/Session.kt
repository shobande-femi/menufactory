package com.shobande.menufactory.session

import com.shobande.menufactory.exceptions.ReservedStateName
import java.util.UUID

/**
 * Session handler
 *
 * @property startStateName name of the start state.
 * This name will be a reserved for the start state only. Any other state definition that tries to use this name
 * will throw a [ReservedStateName] exception.
 * @property previousStateKey a random string used as the key when storing the previous state name in the session
 */
interface Session {
    val startStateName: String

    companion object {
        val previousStateKey: String = UUID.randomUUID().toString()
    }

    /**
     * Initializes a new session.
     * Invoked internally before any state is run.
     *
     * Note: In your implementation if there is an active session for the specified sessionId, skipped initialization
     *
     * @param sessionId user's session Id
     */
    fun start(sessionId: String)

    /**
     * Stores a key-value pair in the current user session
     *
     * @param sessionId user's session Id
     * @param key
     * @param value
     */
    fun set(sessionId: String, key: String, value: Any)

    /**
     * Retrieves the value for a given key from the current user session
     *
     * @param sessionId user's session Id
     * @param key
     */
    fun get(sessionId: String, key: String): Any?

    /**
     * Deletes a user session. Invoked internally by final states
     *
     * @param sessionId user's session Id
     */
    fun end(sessionId: String)

    /**
     * Get the previously visited state for a specific sessionId.
     * This method uses [previousStateKey] as the key when calling [get]
     *
     * @param sessionId user's session Id
     *
     * @return the name of the previously visited state
     */
    fun getPreviousStateName(sessionId: String): String {
        return get(sessionId, previousStateKey) as String? ?: startStateName
    }

    /**
     * Save the previously visited state name in the session.
     * This method uses [previousStateKey] as the key when calling [set]
     *
     * @param sessionId user's session Id
     * @param value name of the previously visted state
     */
    fun setPreviousStateName(sessionId: String, value: String) {
        set(sessionId, previousStateKey, value)
    }
}