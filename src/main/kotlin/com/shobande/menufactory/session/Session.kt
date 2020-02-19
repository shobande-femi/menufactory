package com.shobande.menufactory.session

import java.util.*

/**
 * @property startStateName name of the start state
 */
interface Session {
    val startStateName: String

    companion object {
        val previousStateKey: String = UUID.randomUUID().toString()
    }

    fun start(sessionId: String)

    fun set(sessionId: String, key: String, value: Any)

    fun get(sessionId: String, key: String): Any?

    fun end(sessionId: String)

    fun getPreviousStateName(sessionId: String): String {
        return get(sessionId, previousStateKey) as String? ?: startStateName
    }

    fun setPreviousStateName(sessionId: String, value: String) {
        set(sessionId, previousStateKey, value)
    }
}