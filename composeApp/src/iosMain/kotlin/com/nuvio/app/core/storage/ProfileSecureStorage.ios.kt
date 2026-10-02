package com.nuvio.app.core.storage

import com.nuvio.app.core.sync.migrateCredential
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.NSUserDefaults
import platform.Security.*

/** Device-only Keychain values; uses existing profile-scoped key names as accounts. */
@OptIn(ExperimentalForeignApi::class)
internal object ProfileSecureStorage {
    fun load(key: String): String? = migrateCredential(
        { read(key) }, { NSUserDefaults.standardUserDefaults.stringForKey(key) },
        { save(key, it) }, { NSUserDefaults.standardUserDefaults.removeObjectForKey(key) },
    )
    private fun read(key: String): String? = query(key) { query ->
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status == errSecItemNotFound) return@memScoped null
            check(status == errSecSuccess) { "Secure credential unavailable" }
            val data: CFDataRef = result.value?.reinterpret() ?: error("Secure credential unavailable")
            try {
                val length = CFDataGetLength(data).toInt()
                val bytes = CFDataGetBytePtr(data) ?: return@memScoped ""
                ByteArray(length) { bytes[it].toByte() }.decodeToString()
            } finally { CFRelease(data) }
        }
    }
    fun save(key: String, value: String) = query(key) { query ->
        val bytes = value.encodeToByteArray().toUByteArray()
        val data = CFDataCreate(null, if (bytes.isEmpty()) null else bytes.refTo(0), bytes.size.toLong()) ?: error("Credential encoding failed")
        val updates = CFDictionaryCreateMutable(null, 0, null, null) ?: error("Credential update failed")
        try {
            CFDictionarySetValue(updates, kSecValueData, data)
            val status = SecItemUpdate(query, updates)
            if (status == errSecItemNotFound) {
                CFDictionarySetValue(query, kSecValueData, data)
                CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                check(SecItemAdd(query, null) == errSecSuccess) { "Secure credential save failed" }
            } else check(status == errSecSuccess) { "Secure credential save failed" }
            check(read(key) == value) { "Secure credential verification failed" }
            NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
        } finally { CFRelease(updates); CFRelease(data) }
    }
    fun remove(key: String) = query(key) { query ->
        val status = SecItemDelete(query)
        check(status == errSecSuccess || status == errSecItemNotFound) { "Secure credential removal failed" }
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
    }
    private inline fun <T> query(key: String, block: (CFMutableDictionaryRef) -> T): T {
        val service = CFStringCreateWithCString(null, "com.nuvio.media.profile-credentials", kCFStringEncodingUTF8) ?: error("Credential service unavailable")
        val account = CFStringCreateWithCString(null, key, kCFStringEncodingUTF8) ?: error("Credential account unavailable")
        val query = CFDictionaryCreateMutable(null, 0, null, null) ?: error("Credential query unavailable")
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrAccount, account)
            return block(query)
        } finally { CFRelease(query); CFRelease(account); CFRelease(service) }
    }
}
