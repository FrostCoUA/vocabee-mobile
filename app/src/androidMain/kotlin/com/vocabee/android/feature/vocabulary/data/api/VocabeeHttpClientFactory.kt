package com.vocabee.android.feature.vocabulary.data.api

import com.vocabee.android.core.analytics.AnalyticsTracker
import com.vocabee.android.core.analytics.NoAnalyticsTracker
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object VocabeeHttpClientFactory {
    fun create(
        debugLogging: Boolean = false,
        analyticsTracker: AnalyticsTracker = NoAnalyticsTracker,
    ): HttpClient = HttpClient(OkHttp) {
        install(analyticsHttpPlugin(analyticsTracker))
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        if (debugLogging) {
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("VocabeeHttp", message)
                    }
                }
            }
        }
        expectSuccess = true
    }
}
