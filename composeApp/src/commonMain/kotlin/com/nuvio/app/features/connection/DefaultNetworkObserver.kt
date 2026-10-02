package com.nuvio.app.features.connection

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Follows the default network through one system callback, so reading its kind on the stream
 * load and playback paths is a field read rather than a binder call into the system server.
 * [generation] changes whenever the default network changes; a measurement that spans a
 * change describes neither network and is dropped.
 */
internal expect object DefaultNetworkObserver {
    var kind: NetworkKind?
        private set
    var generation: Int
        private set

    fun start()
}
