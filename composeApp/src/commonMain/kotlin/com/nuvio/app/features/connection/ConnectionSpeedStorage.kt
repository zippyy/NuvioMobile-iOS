package com.nuvio.app.features.connection

/**
 * Platform-specific key-value storage for [ConnectionSpeedEstimator].
 *
 * Android backs with SharedPreferences, iOS backs with NSUserDefaults.
 */
internal expect object ConnectionSpeedStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}
