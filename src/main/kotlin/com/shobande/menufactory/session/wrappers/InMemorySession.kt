package com.shobande.menufactory.session.wrappers

import com.shobande.menufactory.session.Session

class InMemorySession(override val startStateName: String = "__START__") : Session {

    private val session = mutableMapOf<String, MutableMap<String, Any>>()

    override fun start(sessionId: String) {
        if (!session.containsKey(sessionId)) session[sessionId] = mutableMapOf()
    }

    override fun set(sessionId: String, key: String, value: Any) {
        session[sessionId]?.set(key, value)
    }

    override fun get(sessionId: String, key: String): Any? {
        return session[sessionId]?.get(key)
    }

    override fun end(sessionId: String) {
        session.remove(sessionId)
    }
}