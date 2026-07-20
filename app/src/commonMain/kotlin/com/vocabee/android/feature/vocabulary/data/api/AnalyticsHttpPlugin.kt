package com.vocabee.android.feature.vocabulary.data.api

import com.vocabee.android.core.analytics.AnalyticsTracker
import com.vocabee.android.core.platform.currentEpochMillis
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.encodedPath

/**
 * Одна подія `client_api_request` на кожен запит до гейтвея: шлях, метод,
 * статус і тривалість; мережеві збої — з типом винятку. Query string не
 * логуватимемо — текст пошуку живе в окремій події `translation_search`.
 */
fun analyticsHttpPlugin(tracker: AnalyticsTracker) =
    createClientPlugin("VocabeeAnalyticsHttp") {
        on(Send) { request ->
            val startedAt = currentEpochMillis()
            val base = {
                mutableMapOf<String, Any?>(
                    "path" to request.url.encodedPath,
                    "method" to request.method.value,
                    "duration_ms" to (currentEpochMillis() - startedAt),
                )
            }
            try {
                val call = proceed(request)
                tracker.track(
                    "client_api_request",
                    base().apply { put("status_code", call.response.status.value) },
                )
                call
            } catch (cause: ResponseException) {
                tracker.track(
                    "client_api_request",
                    base().apply {
                        put("status_code", cause.response.status.value)
                        put("error_type", cause::class.simpleName)
                    },
                )
                throw cause
            } catch (cause: Throwable) {
                tracker.track(
                    "client_api_request",
                    base().apply { put("error_type", cause::class.simpleName) },
                )
                throw cause
            }
        }
    }
