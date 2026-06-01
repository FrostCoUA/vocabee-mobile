package com.vocabee.android.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse

/**
 * Ktor-backed [VocabeeApi]. Constructed with the platform HTTP client (built by
 * `VocabeeHttpClientFactory.create(...)` on Android).
 */
class KtorVocabeeApi(
    private val client: HttpClient,
    private val config: VocabeeApiConfig,
    private val tokenStore: AuthTokenStore,
) : VocabeeApi {

    override suspend fun search(
        query: String,
        speakLang: String,
        learnLang: String,
    ): SearchResponse {
        val response = try {
            client.get("${config.baseUrl}/v1/search") {
                parameter("q", query)
                parameter("speak", speakLang)
                parameter("learn", learnLang)
                tokenStore.current()?.let { token -> bearerAuth(token) }
            }
        } catch (cause: ClientRequestException) {
            throw cause.toApiException()
        } catch (cause: ServerResponseException) {
            throw cause.toApiException()
        } catch (cause: ResponseException) {
            throw cause.toApiException()
        }
        return response.body()
    }

    private suspend fun ResponseException.toApiException(): VocabeeApiException {
        val body = runCatching { response.body<ApiErrorBody>() }.getOrNull()
        return VocabeeApiException(
            statusCode = response.statusValue(),
            errorType = body?.errorType,
            errorMessage = body?.message ?: response.statusValueDescription(),
        )
    }
}

private fun HttpResponse.statusValue(): Int = status.value
private fun HttpResponse.statusValueDescription(): String = status.description
