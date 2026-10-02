package com.nuvio.app.features.profiles


actual object ProfilePinCacheStorage {
    actual fun loadPayload(profileIndex: Int): String? =
        com.nuvio.app.core.storage.ProfileSecureStorage.load(payloadKey(profileIndex))

    actual fun savePayload(profileIndex: Int, payload: String) {
        com.nuvio.app.core.storage.ProfileSecureStorage.save(payloadKey(profileIndex), payload)
    }

    actual fun removePayload(profileIndex: Int) {
        com.nuvio.app.core.storage.ProfileSecureStorage.remove(payloadKey(profileIndex))
    }

    private fun payloadKey(profileIndex: Int): String = "profile_pin_cache_$profileIndex"
}