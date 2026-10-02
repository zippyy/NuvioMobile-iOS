package com.nuvio.app.features.connection

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import platform.Foundation.NSUserDefaults

internal actual object ConnectionSpeedStorage {
    actual fun getString(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(key)

    actual fun putString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey(key)

    actual fun putBoolean(key: String, value: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(value, forKey = key)
    }
}
