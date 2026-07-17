package com.vocabee.android.feature.vocabulary.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

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

    override suspend fun buildContextGlossary(
        sentence: String,
        sourceLang: String,
        targetLang: String,
    ): ContextGlossaryResponse {
        val response = try {
            client.post("${config.baseUrl}/v1/search/context-glossary") {
                contentType(ContentType.Application.Json)
                setBody(
                    ContextGlossaryRequest(
                        sentence = sentence,
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                    ),
                )
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

    override suspend fun loginWithGoogle(
        idToken: String,
        speakLang: String?,
        learnLang: String?,
    ): AuthTokensResponse {
        val response = try {
            client.post("${config.baseUrl}/v1/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(
                    GoogleAuthRequest(
                        idToken = idToken,
                        speakLang = speakLang,
                        learnLang = learnLang,
                    ),
                )
            }
        } catch (cause: ClientRequestException) {
            throw cause.toApiException()
        } catch (cause: ServerResponseException) {
            throw cause.toApiException()
        } catch (cause: ResponseException) {
            throw cause.toApiException()
        }
        val tokens = response.body<AuthTokensResponse>()
        tokenStore.set(tokens)
        return tokens
    }

    override suspend fun currentUser(): UserResponse {
        val response = try {
            client.get("${config.baseUrl}/v1/auth/me") {
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

    override suspend fun updateCurrentUser(request: UpdateProfileRequest): UserResponse {
        val response = try {
            client.patch("${config.baseUrl}/v1/me") {
                contentType(ContentType.Application.Json)
                setBody(request)
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

    override suspend fun refreshSession(refreshToken: String): AuthTokensResponse {
        val response = try {
            client.post("${config.baseUrl}/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }
        } catch (cause: ClientRequestException) {
            throw cause.toApiException()
        } catch (cause: ServerResponseException) {
            throw cause.toApiException()
        } catch (cause: ResponseException) {
            throw cause.toApiException()
        }
        val tokens = response.body<AuthTokensResponse>()
        tokenStore.set(tokens)
        return tokens
    }

    override suspend fun syncTopics(since: String?): SyncResponse {
        val response = try {
            client.post("${config.baseUrl}/v1/topics/sync") {
                contentType(ContentType.Application.Json)
                setBody(SyncRequest(since))
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

    override suspend fun applySync(request: ApplySyncRequest): SyncResponse {
        val response = try {
            client.post("${config.baseUrl}/v1/topics/sync/apply") {
                contentType(ContentType.Application.Json)
                setBody(request)
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

    override suspend fun claimRewardedAdBees(): UserResponse {
        val response = try {
            client.post("${config.baseUrl}/v1/wallet/rewarded-ad") {
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

    override suspend fun fetchReferral(): ReferralResponse {
        val response = try {
            client.get("${config.baseUrl}/v1/referral/me") {
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

    override suspend fun submitSupport(request: SupportRequestBody): SupportResponse {
        val response = try {
            client.post("${config.baseUrl}/v1/support") {
                contentType(ContentType.Application.Json)
                setBody(request)
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
