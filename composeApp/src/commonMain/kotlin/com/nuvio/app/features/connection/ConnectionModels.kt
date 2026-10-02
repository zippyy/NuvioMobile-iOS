package com.nuvio.app.features.connection

import kotlinx.serialization.Serializable

/** Coarse network class. Throughput is learned separately for each one. */
@Serializable
internal enum class NetworkKind {
    WIFI,
    CELLULAR,
    OTHER,
}

@Serializable
internal data class ConnectionSpeedSample(
    val network: NetworkKind,
    val mbps: Double,
    val recordedAtMs: Long,
)
