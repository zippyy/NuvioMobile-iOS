package com.nuvio.app.features.player

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

internal actual fun createSubtitleHttpClient(): HttpClient = HttpClient(Darwin) {
    followRedirects = false
    expectSuccess = false
    install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 20_000; socketTimeoutMillis = 20_000 }
    engine {
        configureSession {
            HTTPShouldSetCookies = false
            HTTPCookieStorage = null
            URLCredentialStorage = null
            URLCache = null
        }
    }
}
