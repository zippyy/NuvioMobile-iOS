@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nuvio.app.features.connection

import kotlin.concurrent.Volatile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

/**
 * Follows the default network through NWPathMonitor, so reading its kind on the stream
 * load and playback paths is a field read rather than a system call.
 * [generation] changes whenever the default network changes; a measurement that spans a
 * change describes neither network and is dropped.
 */
internal actual object DefaultNetworkObserver {
    @Volatile
    actual var kind: NetworkKind? = null
        private set

    @Volatile
    actual var generation: Int = 0
        private set

    private val lock = SynchronizedObject()
    private var isStarted = false

    actual fun start() {
        synchronized(lock) {
            if (isStarted) return
            isStarted = true

            val pathMonitor = nw_path_monitor_create()

            nw_path_monitor_set_update_handler(pathMonitor) { path ->
                // Keep path with its inferred NSObject? type — pass directly to NW functions
                val newKind: NetworkKind?
                if (path == null) {
                    newKind = null
                } else {
                    val status = nw_path_get_status(path)
                    if (status != nw_path_status_satisfied && status != nw_path_status_satisfiable) {
                        newKind = null
                    } else {
                        val isWifi = nw_path_uses_interface_type(path, nw_interface_type_wifi) ||
                            nw_path_uses_interface_type(path, nw_interface_type_wired)
                        val isCellular = nw_path_uses_interface_type(path, nw_interface_type_cellular)
                        newKind = when {
                            isWifi -> NetworkKind.WIFI
                            isCellular -> NetworkKind.CELLULAR
                            else -> NetworkKind.OTHER
                        }
                    }
                }
                synchronized(lock) {
                    if (newKind != kind) {
                        kind = newKind
                        generation++
                    }
                }
            }

            nw_path_monitor_set_queue(pathMonitor, dispatch_get_main_queue())
            nw_path_monitor_start(pathMonitor)
        }
    }
}
