package com.nuvio.app.core.sync

/** Session-local retry protection supplements the server's persisted PIN lock. */
internal class ProfilePinThrottle {
    private data class Attempts(val count: Int, val lockedUntil: Long)
    private val attempts = mutableMapOf<String, Attempts>()
    fun allowed(scope: String, now: Long): Boolean = now >= (attempts[scope]?.lockedUntil ?: 0)
    fun failed(scope: String, now: Long) {
        val count = (attempts[scope]?.count ?: 0) + 1
        attempts[scope] = Attempts(count, if (count >= 5) now + 30_000 else 0)
    }
    fun succeeded(scope: String) { attempts.remove(scope) }
}
