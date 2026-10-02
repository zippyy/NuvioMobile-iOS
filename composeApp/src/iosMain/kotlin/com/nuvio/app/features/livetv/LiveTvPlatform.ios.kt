@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.nuvio.app.features.livetv

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.call.body
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Security.*
import platform.Foundation.*
import platform.zlib.*

@Composable
actual fun rememberLiveTvStore(): LiveTvStore = IosLiveTvStore
actual fun liveTvNow(): Long=(NSDate().timeIntervalSince1970*1000).toLong()
actual fun liveTvTimeLabel(epochMs: Long): String=NSDateFormatter().apply { dateFormat="HH:mm" }.stringFromDate(NSDate(timeIntervalSince1970=epochMs/1000.0))
private val liveHttp=HttpClient(Darwin) {
    followRedirects=false; expectSuccess=false
    install(HttpTimeout) { connectTimeoutMillis=15_000; socketTimeoutMillis=120_000; requestTimeoutMillis=120_000 }
}
actual suspend fun liveTvHttpText(url: String, headers: Map<String,String>): String {
    var address=requireLiveTvUrl(url); var requestHeaders=safeLiveTvHeaders(headers)
    repeat(6) {
        val response=liveHttp.get(address) { requestHeaders.forEach { (k,v)->header(k,v) } }
        if(response.status.value in listOf(301,302,303,307,308)) {
            val location=response.headers["Location"]?:throw LiveTvHttpException(response.status.value)
            val next=NSURL(string=location,relativeToURL=NSURL(string=address)).absoluteURL?.absoluteString?:error("Invalid redirect")
            requestHeaders=liveTvRedirectHeaders(address,next,requestHeaders); address=next
        } else {
            if(response.status.value !in 200..299) throw LiveTvHttpException(response.status.value)
            val bytes=response.body<ByteArray>()
            require(bytes.size<=128*1024*1024) { "Response too large" }
            return gunzipLiveTv(bytes).decodeToString()
        }
    }
    error("Too many redirects")
}
/** Handles XMLTV .gz even when the server omits Content-Encoding. */
internal fun gunzipLiveTv(input: ByteArray): ByteArray {
    if(input.size<2 || input[0]!=0x1f.toByte() || input[1]!=0x8b.toByte()) return input
    return memScoped {
        val stream=alloc<z_stream>(); platform.posix.memset(stream.ptr,0,sizeOf<z_stream>().toULong())
        check(inflateInit2_(stream.ptr,31,zlibVersion(),sizeOf<z_stream>().toInt())==Z_OK)
        try {
            input.usePinned { pinned ->
                stream.next_in=pinned.addressOf(0).reinterpret(); stream.avail_in=input.size.toUInt()
                val chunks=mutableListOf<ByteArray>(); var total=0; var status: Int
                do {
                    val chunk=ByteArray(64*1024)
                    chunk.usePinned { output ->
                        stream.next_out=output.addressOf(0).reinterpret(); stream.avail_out=chunk.size.toUInt()
                        status=inflate(stream.ptr,Z_NO_FLUSH)
                    }
                    check(status==Z_OK || status==Z_STREAM_END) { "Invalid compressed guide" }
                    val count=chunk.size-stream.avail_out.toInt(); total+=count
                    require(total<=128*1024*1024) { "Guide exceeds decompression limit" }
                    chunks+=chunk.copyOf(count)
                } while(status!=Z_STREAM_END)
                ByteArray(total).also { result->var offset=0; chunks.forEach { it.copyInto(result,offset); offset+=it.size } }
            }
        } finally { inflateEnd(stream.ptr) }
    }
}
private object IosLiveTvStore: LiveTvStore {
    private fun cachePath(key: String): String {
        val base=NSSearchPathForDirectoriesInDomains(NSCachesDirectory,NSUserDomainMask,true).first() as String
        return "$base/nuvio-live-$key.json"
    }
    override fun read(key: String): String? {
        if(key.startsWith("guide-")) return NSString.stringWithContentsOfFile(cachePath(key),NSUTF8StringEncoding,null)
        return query(key) { q ->
            CFDictionarySetValue(q,kSecReturnData,kCFBooleanTrue); CFDictionarySetValue(q,kSecMatchLimit,kSecMatchLimitOne)
            memScoped {
                val result=alloc<CFTypeRefVar>(); val status=SecItemCopyMatching(q,result.ptr)
                if(status==errSecItemNotFound) return@memScoped null
                check(status==errSecSuccess) { "Unable to read protected Live TV settings" }
                val data: CFDataRef=result.value?.reinterpret()?:return@memScoped null
                try { val count=CFDataGetLength(data).toInt(); val bytes=CFDataGetBytePtr(data)?:return@memScoped null
                    ByteArray(count) { bytes[it].toByte() }.decodeToString()
                } finally { CFRelease(data) }
            }
        }
    }
    override fun write(key: String,value: String) {
        if(key.startsWith("guide-")) {
            val bytes=value.encodeToByteArray()
            val data=bytes.usePinned { NSData.create(bytes=it.addressOf(0),length=bytes.size.toULong()) }
            check(data.writeToFile(cachePath(key),NSDataWritingAtomic or NSDataWritingFileProtectionCompleteUntilFirstUserAuthentication,null)) { "Unable to save programme guide" }
            return
        }
        query(key) { q ->
            val bytes=value.encodeToByteArray().toUByteArray()
            val data=CFDataCreate(null,bytes.refTo(0),bytes.size.toLong())?:error("Cannot encode Live TV settings")
            val update=CFDictionaryCreateMutable(null,0,null,null)?:error("Cannot create Keychain update")
            try {
                CFDictionarySetValue(update,kSecValueData,data)
                val status=SecItemUpdate(q,update)
                if(status==errSecItemNotFound) {
                    CFDictionarySetValue(q,kSecValueData,data)
                    CFDictionarySetValue(q,kSecAttrAccessible,kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
                    check(SecItemAdd(q,null)==errSecSuccess) { "Unable to save protected Live TV settings" }
                } else check(status==errSecSuccess) { "Unable to update protected Live TV settings" }
            } finally { CFRelease(update); CFRelease(data) }
        }
    }
    private inline fun <T> query(key: String, block: (CFMutableDictionaryRef)->T): T {
        val service=CFStringCreateWithCString(null,"com.nuvio.media.livetv",kCFStringEncodingUTF8)?:error("Keychain service")
        val account=CFStringCreateWithCString(null,key,kCFStringEncodingUTF8)?:error("Keychain account")
        val q=CFDictionaryCreateMutable(null,0,null,null)?:error("Keychain query")
        try {
            CFDictionarySetValue(q,kSecClass,kSecClassGenericPassword); CFDictionarySetValue(q,kSecAttrService,service); CFDictionarySetValue(q,kSecAttrAccount,account)
            return block(q)
        } finally { CFRelease(q); CFRelease(account); CFRelease(service) }
    }
}
