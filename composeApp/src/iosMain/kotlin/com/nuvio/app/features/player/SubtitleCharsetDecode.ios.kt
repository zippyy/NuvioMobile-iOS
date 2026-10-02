@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.nuvio.app.features.player

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFStringConvertEncodingToNSStringEncoding
import platform.CoreFoundation.CFStringConvertIANACharSetNameToEncoding
import platform.CoreFoundation.kCFStringEncodingInvalidId
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.kCFStringEncodingUTF8
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import platform.Foundation.NSString
import platform.Foundation.create

internal actual fun decodeSubtitleCharset(bytes: ByteArray, offset: Int, length: Int, charset: String): String? {
    require(offset >= 0 && length >= 0 && offset <= bytes.size - length)
    if (length == 0) return ""
    val name = CFStringCreateWithCString(null, charset, kCFStringEncodingUTF8) ?: return null
    val encoding = try { CFStringConvertIANACharSetNameToEncoding(name) }
        finally { platform.CoreFoundation.CFRelease(name) }
    if (encoding == kCFStringEncodingInvalidId) return null
    return bytes.usePinned {
        NSString.create(bytes = it.addressOf(offset), length = length.toULong(),
            encoding = CFStringConvertEncodingToNSStringEncoding(encoding))?.toString()
    }
}
