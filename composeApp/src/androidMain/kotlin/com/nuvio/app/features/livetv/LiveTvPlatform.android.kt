package com.nuvio.app.features.livetv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@Composable
actual fun rememberLiveTvStore(): LiveTvStore {
    val context=LocalContext.current.applicationContext
    return remember(context) { AndroidLiveTvStore(context) }
}
actual fun liveTvNow(): Long=System.currentTimeMillis()
actual fun liveTvTimeLabel(epochMs: Long): String=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date(epochMs))
private val liveClient=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(15,TimeUnit.SECONDS).readTimeout(120,TimeUnit.SECONDS).build()
actual suspend fun liveTvHttpText(url: String,headers: Map<String,String>): String=withContext(Dispatchers.IO) {
    var address=requireLiveTvUrl(url); var requestHeaders=safeLiveTvHeaders(headers)
    repeat(6) {
        val request=Request.Builder().url(address).apply { requestHeaders.forEach { (k,v)->header(k,v) } }.build()
        liveClient.newCall(request).execute().use { response->
            if(response.code in listOf(301,302,303,307,308)) {
                val next=response.header("Location")?.let { request.url.resolve(it) }?.toString()?:throw LiveTvHttpException(response.code)
                requestHeaders=liveTvRedirectHeaders(address,next,requestHeaders); address=next
            } else {
                if(!response.isSuccessful) throw LiveTvHttpException(response.code)
                val input=response.body?.byteStream()?:error("Empty response")
                val buffered=input.buffered(); buffered.mark(2); val gzip=buffered.read()==31 && buffered.read()==139; buffered.reset()
                val decoded=if(gzip)GZIPInputStream(buffered) else buffered
                val bytes=decoded.readNBytes(128*1024*1024+1); require(bytes.size<=128*1024*1024) { "Response too large" }
                return@withContext bytes.decodeToString()
            }
        }
    }
    error("Too many redirects")
}
private class AndroidLiveTvStore(context: Context): LiveTvStore {
    private val preferences=context.getSharedPreferences("nuvio_live_tv",Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val alias="nuvio-live-tv"
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias,null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun read(key: String): String? {
        val raw=preferences.getString(key,null)?:return null
        return runCatching {
            val bytes=Base64.decode(raw,Base64.NO_WRAP); val iv=bytes.copyOfRange(0,12)
            Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv)) }.doFinal(bytes.copyOfRange(12,bytes.size)).decodeToString()
        }.getOrNull()
    }
    override fun write(key: String,value: String) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val data=cipher.iv+cipher.doFinal(value.encodeToByteArray())
        check(preferences.edit().putString(key,Base64.encodeToString(data,Base64.NO_WRAP)).commit()) { "Unable to save Live TV settings" }
    }
}
