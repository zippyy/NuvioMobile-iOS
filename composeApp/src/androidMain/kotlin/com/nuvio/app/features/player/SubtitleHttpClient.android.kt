package com.nuvio.app.features.player

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

internal actual fun createSubtitleHttpClient(): HttpClient = HttpClient(OkHttp) {
    followRedirects = false
    expectSuccess = false
    install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 20_000; socketTimeoutMillis = 20_000 }
    engine { config { followRedirects(false); followSslRedirects(false); cookieJar(okhttp3.CookieJar.NO_COOKIES) } }
}
