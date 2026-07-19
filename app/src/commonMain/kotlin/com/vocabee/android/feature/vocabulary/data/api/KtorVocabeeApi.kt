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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ktor-backed [VocabeeApi]. Constructed with the platform HTTP client (built by
 * `VocabeeHttpClientFactory.create(...)` on Android).
 */
class KtorVocabeeApi(
    private val client: HttpClient,
    private val config: VocabeeApiConfig,
    private val tokenStore: AuthTokenStore,
) : VocabeeApi, SessionExpiryObservable {

    override val sessionExpired = tokenStore.sessionExpired

    /**
     * A refresh token can only be used once. Serialising refreshes prevents two
     * simultaneous 401 responses from racing and revoking each other's session.
     */
    private val refreshMutex = Mutex()

    override suspend fun search(
        query: String,
        speakLang: String,
        learnLang: String,
    ): SearchResponse {
        return withFreshAccessToken {
            executeRequest {
                client.get("${config.baseUrl}/v1/search") {
                    parameter("q", query)
                    parameter("speak", speakLang)
                    parameter("learn", learnLang)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun submitQualityFeedback(
        request: QualityFeedbackRequest,
    ): QualityFeedbackResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/translation-feedback") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun buildContextGlossary(
        sentence: String,
        sourceLang: String,
        targetLang: String,
    ): ContextGlossaryResponse {
        return withFreshAccessToken {
            executeRequest {
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
                }.body()
            }
        }
    }

    override suspend fun loginWithGoogle(
        idToken: String,
        speakLang: String?,
        learnLang: String?,
    ): AuthTokensResponse {
        val tokens = executeRequest {
            client.post("${config.baseUrl}/v1/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(
                    GoogleAuthRequest(
                        idToken = idToken,
                        speakLang = speakLang,
                        learnLang = learnLang,
                    ),
                )
            }.body<AuthTokensResponse>()
        }
        tokenStore.set(tokens)
        return tokens
    }

    override suspend fun currentUser(): UserResponse {
        return withFreshAccessToken {
            executeRequest {
                client.get("${config.baseUrl}/v1/auth/me") {
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun updateCurrentUser(request: UpdateProfileRequest): UserResponse {
        return withFreshAccessToken {
            executeRequest {
                client.patch("${config.baseUrl}/v1/me") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun refreshSession(refreshToken: String): AuthTokensResponse {
        val tokens = executeRequest {
            client.post("${config.baseUrl}/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }.body<AuthTokensResponse>()
        }
        tokenStore.set(tokens)
        return tokens
    }

    override suspend fun syncTopics(since: String?): SyncResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/topics/sync") {
                    contentType(ContentType.Application.Json)
                    setBody(SyncRequest(since))
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun applySync(request: ApplySyncRequest): SyncResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/topics/sync/apply") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun claimRewardedAdBees(): UserResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/wallet/rewarded-ad") {
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun fetchReferral(): ReferralResponse {
        return withFreshAccessToken {
            executeRequest {
                client.get("${config.baseUrl}/v1/referral/me") {
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun submitSupport(request: SupportRequestBody): SupportResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/support") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    /** Replays one request after renewing an expired access token. */
    private suspend fun <T> withFreshAccessToken(request: suspend () -> T): T {
        val failedAccessToken = tokenStore.current() ?: return request()
        return try {
            request()
        } catch (cause: VocabeeApiException) {
            if (cause.statusCode != 401) throw cause
            refreshAccessTokenAfter401(failedAccessToken)
            request()
        }
    }

    private suspend fun refreshAccessTokenAfter401(failedAccessToken: String) {
        refreshMutex.withLock {
            // Another concurrent request has already completed the rotation.
            if (tokenStore.current() != failedAccessToken) return
            val refreshToken = tokenStore.refreshToken() ?: throw VocabeeApiException(
                statusCode = 401,
                errorType = "unauthorized",
                errorMessage = "Потрібна повторна авторизація.",
            )
            try {
                refreshSession(refreshToken)
            } catch (cause: VocabeeApiException) {
                if (cause.statusCode == 401) tokenStore.clear()
                throw cause
            }
        }
    }

    private suspend fun <T> executeRequest(request: suspend () -> T): T = try {
        request()
    } catch (cause: ClientRequestException) {
        throw cause.toApiException()
    } catch (cause: ServerResponseException) {
        throw cause.toApiException()
    } catch (cause: ResponseException) {
        throw cause.toApiException()
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
